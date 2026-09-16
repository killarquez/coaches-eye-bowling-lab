#!/usr/bin/env python3
"""
mine_and_label.py - Automated Bowling Footage Mining and Silver-Standard Auto-Annotation Tool

Extracts and labels bowling ball tracking datasets from raw lane footage (MP4/MOV).
Implements:
1. Temporal motion energy gating to sample frames strictly during delivery (foul line to pin deck).
2. Lane trapezoid masking and circularity filtering for silver-standard ball annotation.
3. Hard negative sample mining (bowler shoes, pin sweep bars, ball return hoods).
4. Direct export to standard YOLO training layout (train/val images and labels, data.yaml).
"""

import argparse
import glob
import math
import os
import random
import sys
from pathlib import Path
from typing import List, Optional, Tuple

import cv2
import numpy as np


def parse_args():
    parser = argparse.ArgumentParser(
        description="Mine and auto-annotate bowling footage for LiteRT / YOLO ball tracking"
    )
    parser.add_argument(
        "--input-dir",
        type=str,
        default="videos",
        help="Directory containing bowling video recordings (.mp4, .mov, etc.)",
    )
    parser.add_argument(
        "--output-dir",
        type=str,
        default="dataset",
        help="Root directory for output YOLO dataset",
    )
    parser.add_argument(
        "--img-size",
        type=int,
        default=416,
        help="Down-sampled square resolution (default: 416 for 416x416 grayscale)",
    )
    parser.add_argument(
        "--val-split",
        type=float,
        default=0.20,
        help="Validation split ratio (default: 0.20 = 20%% val)",
    )
    parser.add_argument(
        "--frame-stride",
        type=int,
        default=1,
        help="Frame sampling stride during active delivery (default: 1 = every frame)",
    )
    parser.add_argument(
        "--activity-threshold",
        type=float,
        default=25.0,
        help="Mean pixel motion energy threshold to flag delivery activity",
    )
    parser.add_argument(
        "--max-negatives-per-video",
        type=int,
        default=120,
        help="Maximum hard negative frames (shoes, sweeps, return hoods) to extract per clip",
    )
    parser.add_argument(
        "--visualize",
        action="store_true",
        help="Save debug overlay visualization alongside output images",
    )
    parser.add_argument(
        "--max-videos",
        type=int,
        default=None,
        help="Limit number of videos to process (useful for quick pilot verification)",
    )
    parser.add_argument(
        "--generate-synthetic-sample",
        action="store_true",
        help="Generate a synthetic bowling video to test the pipeline end-to-end",
    )
    return parser.parse_args()


class TemporalDeliveryDetector:
    """
    Detects bowling shot delivery windows using motion differencing energy on the lane.
    Filters out static frames and frames where the bowler is only moving on the approach.
    """

    def __init__(self, activity_threshold: float = 12.0, min_delivery_frames: int = 8):
        self.activity_threshold = activity_threshold
        self.min_delivery_frames = min_delivery_frames
        self.prev_gray: Optional[np.ndarray] = None
        self.in_delivery = False
        self.active_counter = 0

    def process_frame(self, gray_frame: np.ndarray) -> Tuple[bool, float]:
        """
        Returns (is_active_delivery, motion_energy)
        """
        if self.prev_gray is None:
            self.prev_gray = gray_frame.copy()
            return False, 0.0

        # Inter-frame absolute difference
        diff = cv2.absdiff(gray_frame, self.prev_gray)
        self.prev_gray = gray_frame.copy()

        # Measure motion energy strictly on the active lane surface (foul line to pin deck)
        # Avoids triggering on bowler approach steps behind the foul line
        h, w = gray_frame.shape
        lane_roi = diff[int(h * 0.37) : int(h * 0.56), int(w * 0.12) : int(w * 0.88)]
        motion_energy = float(np.mean(lane_roi))

        if motion_energy > self.activity_threshold:
            self.active_counter += 1
            if self.active_counter >= 2:
                self.in_delivery = True
        else:
            if self.active_counter > 0:
                self.active_counter -= 1
            if self.active_counter == 0:
                self.in_delivery = False

        return self.in_delivery, motion_energy


class SilverStandardAnnotator:
    """
    Bootstrapping silver-standard annotator for bowling balls.
    Combines background subtraction, lane polygon filtering, aspect ratio,
    and circularity tests to output verified bounding boxes.
    """

    def __init__(self, img_size: int = 416):
        self.img_size = img_size
        self.bg_subtractor = cv2.createBackgroundSubtractorMOG2(
            history=60, varThreshold=24, detectShadows=False
        )

    def get_lane_mask(self, width: int, height: int) -> np.ndarray:
        """
        Returns a trapezoidal lane polygon mask matching the camera perspective:
        Pins at y ~ 0.37, Foul line at y ~ 0.56. Excludes the approach (y > 0.56) and ceiling (y < 0.37).
        """
        mask = np.zeros((height, width), dtype=np.uint8)
        top_left = (int(width * 0.28), int(height * 0.37))
        top_right = (int(width * 0.72), int(height * 0.37))
        bot_right = (int(width * 0.88), int(height * 0.56))
        bot_left = (int(width * 0.08), int(height * 0.56))

        pts = np.array([top_left, top_right, bot_right, bot_left], dtype=np.int32)
        cv2.fillPoly(mask, [pts], 255)
        return mask

    def annotate_frame(
        self, gray_frame: np.ndarray
    ) -> Tuple[Optional[Tuple[float, float, float, float]], Optional[str]]:
        """
        Attempts to detect the bowling ball on the lane surface.
        Returns (bbox_norm, hard_negative_type)
        """
        h, w = gray_frame.shape
        fg_mask = self.bg_subtractor.apply(gray_frame)

        # Apply lane mask
        lane_mask = self.get_lane_mask(w, h)
        masked_fg = cv2.bitwise_and(fg_mask, fg_mask, mask=lane_mask)

        # Morphological opening to remove salt noise
        kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (3, 3))
        clean_fg = cv2.morphologyEx(masked_fg, cv2.MORPH_OPEN, kernel)

        contours, _ = cv2.findContours(
            clean_fg, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE
        )

        candidate_balls = []
        hard_negative = None

        for cnt in contours:
            area = cv2.contourArea(cnt)
            if area < 10:
                continue

            x, y, bw, bh = cv2.boundingRect(cnt)
            perimeter = cv2.arcLength(cnt, True)
            if perimeter <= 0:
                continue

            circularity = 4.0 * math.pi * area / (perimeter * perimeter)
            aspect_ratio = float(bw) / max(1, bh)

            # 1. Hard Negative: Bowler slide foot crossing foul line or bowler torso
            if (area > 350 or circularity < 0.25) and y > int(h * 0.48):
                hard_negative = "shoes"
                continue

            # 2. Hard Negative: Pin sweep arm near pin deck
            if y < int(h * 0.39) and aspect_ratio > 2.5:
                hard_negative = "sweep"
                continue

            # 3. Hard Negative: Ball return hood / gutter edge
            if (x < int(w * 0.14) or (x + bw) > int(w * 0.86)) and aspect_ratio > 1.8:
                hard_negative = "ball_return"
                continue

            # 4. Bowling Ball Criteria:
            # - Down-lane perspective area: 12 to 320 px (8x8 to 18x18 px ball)
            # - Aspect ratio close to 1.0 (0.65 to 1.45)
            # - Circularity >= 0.35
            if 12 <= area <= 320 and 0.65 <= aspect_ratio <= 1.45 and circularity >= 0.35:
                score = circularity - abs(aspect_ratio - 1.0) * 0.4
                candidate_balls.append((score, (x, y, bw, bh)))

        if candidate_balls:
            candidate_balls.sort(key=lambda item: item[0], reverse=True)
            _, (x, y, bw, bh) = candidate_balls[0]

            # Normalized YOLO coordinates (center_x, center_y, width, height)
            cx = (x + bw / 2.0) / w
            cy = (y + bh / 2.0) / h
            norm_w = bw / float(w)
            norm_h = bh / float(h)
            return (cx, cy, norm_w, norm_h), None

        return None, hard_negative


def generate_synthetic_bowling_clip(output_path: str, duration_sec: int = 5, fps: int = 30):
    """
    Generates a synthetic bowling video containing approach, release, ball travel, and pin sweep
    to validate the mining and auto-annotation pipeline end-to-end.
    """
    w, h = 640, 480
    fourcc = cv2.VideoWriter_fourcc(*"mp4v")
    out = cv2.VideoWriter(output_path, fourcc, fps, (w, h), isColor=False)

    total_frames = duration_sec * fps

    # Ball trajectory parameters (screen space)
    # Starts at foul line (bottom center) and rolls towards pin deck (top center)
    start_x, start_y = w // 2 - 40, int(h * 0.90)
    end_x, end_y = w // 2 + 10, int(h * 0.20)

    for i in range(total_frames):
        # Base lane gradient background
        frame = np.full((h, w), 80, dtype=np.uint8)
        # Draw trapezoid lane board pattern
        lane_pts = np.array(
            [[int(w * 0.35), int(h * 0.18)],
             [int(w * 0.65), int(h * 0.18)],
             [int(w * 0.90), int(h * 0.98)],
             [int(w * 0.10), int(h * 0.98)]],
            dtype=np.int32,
        )
        cv2.fillPoly(frame, [lane_pts], 130)

        # Draw gutters
        cv2.line(frame, (int(w * 0.35), int(h * 0.18)), (int(w * 0.10), int(h * 0.98)), 40, 4)
        cv2.line(frame, (int(w * 0.65), int(h * 0.18)), (int(w * 0.90), int(h * 0.98)), 40, 4)

        # Delivery phase: frames 20 to 100
        if 20 <= i <= 100:
            t = (i - 20) / 80.0
            bx = int(start_x + t * (end_x - start_x))
            by = int(start_y + t * (end_y - start_y))
            # Ball radius shrinks with perspective distance
            radius = int(24 * (1.0 - 0.55 * t))
            # Draw spherical bowling ball with bright reflection
            cv2.circle(frame, (bx, by), radius, 25, -1)
            cv2.circle(frame, (bx - radius // 3, by - radius // 3), max(2, radius // 4), 220, -1)

        # Hard negative: bowler sliding shoe near bottom during release
        if 15 <= i <= 40:
            cv2.rectangle(frame, (w // 2 - 80, int(h * 0.92)), (w // 2 - 30, int(h * 0.97)), 30, -1)

        # Hard negative: pin sweep arm descending after shot (frames 110 to 140)
        if 110 <= i <= 140:
            cv2.rectangle(frame, (int(w * 0.33), int(h * 0.22)), (int(w * 0.67), int(h * 0.25)), 210, -1)

        out.write(frame)

    out.release()
    print(f"[+] Synthetic bowling video saved to: {output_path}")


def process_video(
    video_path: Path,
    annotator: SilverStandardAnnotator,
    delivery_detector: TemporalDeliveryDetector,
    dataset_dirs: dict,
    val_split: float,
    img_size: int,
    frame_stride: int,
    max_negatives: int,
    visualize: bool,
) -> Tuple[int, int]:
    """
    Mines and labels a single bowling footage file.
    Returns (positive_count, negative_count)
    """
    cap = cv2.VideoCapture(str(video_path))
    if not cap.isOpened():
        print(f"[-] Warning: Failed to open video file {video_path}")
        return 0, 0

    video_name = video_path.stem
    frame_idx = 0
    pos_extracted = 0
    neg_extracted = 0

    print(f"[*] Processing: {video_path.name}")

    while True:
        ret, frame = cap.read()
        if not ret:
            break

        frame_idx += 1
        # Convert to single-channel grayscale
        if len(frame.shape) == 3:
            gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        else:
            gray = frame

        # Downsample to target square resolution
        resized_gray = cv2.resize(gray, (img_size, img_size), interpolation=cv2.INTER_AREA)

        # Temporal activity check
        is_delivery, energy = delivery_detector.process_frame(resized_gray)

        # Decide whether to sample this frame
        if not is_delivery and neg_extracted >= max_negatives:
            continue

        if frame_idx % frame_stride != 0:
            continue

        # Silver-standard annotation
        bbox_norm, hard_neg_type = annotator.annotate_frame(resized_gray)

        is_val = random.random() < val_split
        split_key = "val" if is_val else "train"

        if bbox_norm is not None:
            # Positive sample (bowling ball detected)
            pos_extracted += 1
            sample_id = f"{video_name}_f{frame_idx:06d}"
            img_out = dataset_dirs[split_key]["images"] / f"{sample_id}.png"
            lbl_out = dataset_dirs[split_key]["labels"] / f"{sample_id}.txt"

            cv2.imwrite(str(img_out), resized_gray)

            # YOLO label format: class_id cx cy w h
            cx, cy, bw, bh = bbox_norm
            with open(lbl_out, "w") as f:
                f.write(f"0 {cx:.6f} {cy:.6f} {bw:.6f} {bh:.6f}\n")

            if visualize:
                vis_img = cv2.cvtColor(resized_gray, cv2.COLOR_GRAY2BGR)
                px_cx, px_cy = int(cx * img_size), int(cy * img_size)
                px_w, px_h = int(bw * img_size), int(bh * img_size)
                x1 = max(0, px_cx - px_w // 2)
                y1 = max(0, px_cy - px_h // 2)
                x2 = min(img_size - 1, px_cx + px_w // 2)
                y2 = min(img_size - 1, px_cy + px_h // 2)
                cv2.rectangle(vis_img, (x1, y1), (x2, y2), (0, 255, 0), 2)
                cv2.circle(vis_img, (px_cx, px_cy), 3, (0, 0, 255), -1)
                vis_out = dataset_dirs[split_key]["images"] / f"{sample_id}_vis.jpg"
                cv2.imwrite(str(vis_out), vis_img)

        elif hard_neg_type is not None and neg_extracted < max_negatives:
            # Negative sample (bowler shoes, sweep, return hood)
            neg_extracted += 1
            sample_id = f"{video_name}_neg_{hard_neg_type}_f{frame_idx:06d}"
            img_out = dataset_dirs[split_key]["images"] / f"{sample_id}.png"
            lbl_out = dataset_dirs[split_key]["labels"] / f"{sample_id}.txt"

            cv2.imwrite(str(img_out), resized_gray)
            # Empty label file designates hard negative in YOLO
            with open(lbl_out, "w") as f:
                pass

    cap.release()
    print(f"    -> Mined {pos_extracted} positive frames, {neg_extracted} hard negative frames.")
    return pos_extracted, neg_extracted


def emit_yolo_yaml(dataset_root: Path):
    """
    Emits data.yaml for Ultralytics YOLO training.
    """
    yaml_path = dataset_root / "data.yaml"
    abs_root = dataset_root.resolve()
    yaml_content = f"""# Bowling Ball Detector Dataset
path: {abs_root.as_posix()}
train: train/images
val: val/images

# Classes
names:
  0: bowling_ball
"""
    with open(yaml_path, "w") as f:
        f.write(yaml_content)
    print(f"[+] YOLO dataset configuration written to: {yaml_path}")


def main():
    args = parse_args()

    input_dir = Path(args.input_dir)
    output_dir = Path(args.output_dir)

    if args.generate_synthetic_sample:
        input_dir.mkdir(parents=True, exist_ok=True)
        synthetic_video = input_dir / "synthetic_bowling_shot.mp4"
        generate_synthetic_bowling_clip(str(synthetic_video))

    if not input_dir.exists():
        print(f"[-] Input directory does not exist: {input_dir}")
        print("[*] Tip: Run with --generate-synthetic-sample to create a test video.")
        sys.exit(1)

    # Prepare YOLO directory structure
    dataset_dirs = {
        "train": {
            "images": output_dir / "train" / "images",
            "labels": output_dir / "train" / "labels",
        },
        "val": {
            "images": output_dir / "val" / "images",
            "labels": output_dir / "val" / "labels",
        },
    }

    for split in ["train", "val"]:
        dataset_dirs[split]["images"].mkdir(parents=True, exist_ok=True)
        dataset_dirs[split]["labels"].mkdir(parents=True, exist_ok=True)

    # Locate video files (including subdirectories)
    video_extensions = ("*.mp4", "*.mov", "*.avi", "*.mkv", "*.m4v")
    video_files: List[Path] = []
    for ext in video_extensions:
        video_files.extend([
            f for f in input_dir.rglob(ext)
            if not f.name.startswith(".") and ".crdownload" not in f.name
        ])
    video_files.sort()

    if not video_files:
        print(f"[-] No video files found in {input_dir}.")
        print("[*] Creating synthetic sample to ensure dataset tooling is validated...")
        synthetic_video = input_dir / "synthetic_bowling_shot.mp4"
        generate_synthetic_bowling_clip(str(synthetic_video))
        video_files.append(synthetic_video)

    if args.max_videos is not None and args.max_videos > 0:
        print(f"[*] Pilot run: Limiting processing to first {args.max_videos} video(s).")
        video_files = video_files[:args.max_videos]

    total_pos = 0
    total_neg = 0

    for idx, video_file in enumerate(video_files, 1):
        annotator = SilverStandardAnnotator(img_size=args.img_size)
        detector = TemporalDeliveryDetector(activity_threshold=args.activity_threshold)

        print(f"[*] [{idx}/{len(video_files)}] Processing: {video_file.name}", flush=True)
        pos, neg = process_video(
            video_path=video_file,
            annotator=annotator,
            delivery_detector=detector,
            dataset_dirs=dataset_dirs,
            val_split=args.val_split,
            img_size=args.img_size,
            frame_stride=args.frame_stride,
            max_negatives=args.max_negatives_per_video,
            visualize=args.visualize,
        )
        total_pos += pos
        total_neg += neg
        print(f"    -> Running total: {total_pos:,} positive frames, {total_neg:,} hard negative frames.\n", flush=True)

    emit_yolo_yaml(output_dir)

    print("\n========================================================")
    print(f"Dataset Curation & Bootstrapping Completed Successfully!")
    print(f"Total positive ball annotations: {total_pos}")
    print(f"Total hard negative frames:      {total_neg}")
    print(f"Output directory:                {output_dir.resolve()}")
    print("========================================================\n")


if __name__ == "__main__":
    main()
