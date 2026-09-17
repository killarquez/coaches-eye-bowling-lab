#!/usr/bin/env python3
"""
mine_multiclass_dataset.py - Multi-Class Automated Bowling Dataset Mining & Labeling Tool

Extracts representative frames across real-world bowling videos (MP4/MOV) and generates
high-quality YOLOv8 annotations for 7 target perception classes:
  0: bowling_ball - Bowling ball in motion down the lane or on approach
  1: pin_rack     - Triangular 10-pin formation at 60 ft
  2: pin          - Individual bowling pins (standing & flying)
  3: foul_line    - Black horizontal boundary at 0 ft
  4: arrows       - Chevron arrow markings at 15 ft
  5: lane         - Active lane surface bounded by left & right gutters
  6: slide_foot   - Bowler's slide shoe at release near foul line

Samples across 4 distinct kinematic delivery phases:
  Phase A: Stance & Approach (static landmarks)
  Phase B: Slide & Release (slide foot + ball release)
  Phase C: Mid-Lane Roll (ball translating down lane)
  Phase D: Pin Impact & Scatter (flying pins + deflection)
"""

import argparse
import glob
import math
import os
import random
import sys
from pathlib import Path
from typing import Dict, List, Optional, Tuple

import cv2
import numpy as np

CLASS_NAMES = [
    "bowling_ball",
    "pin_rack",
    "pin",
    "foul_line",
    "arrows",
    "lane",
    "slide_foot",
]

CLASS_COLORS = [
    (0, 0, 255),    # 0: ball (Red)
    (0, 255, 0),    # 1: pin_rack (Green)
    (0, 255, 255),  # 2: pin (Yellow)
    (255, 0, 0),    # 3: foul_line (Blue)
    (0, 165, 255),  # 4: arrows (Orange)
    (255, 255, 0),  # 5: lane (Cyan)
    (255, 0, 255),  # 6: slide_foot (Magenta)
]


def parse_args():
    parser = argparse.ArgumentParser(
        description="Mine and auto-annotate multi-class bowling footage for YOLOv8 & LiteRT"
    )
    parser.add_argument(
        "--input-dir",
        type=str,
        default="J:/videos",
        help="Directory containing bowling video recordings (.mp4, .mov)",
    )
    parser.add_argument(
        "--output-dir",
        type=str,
        default="dataset_multiclass",
        help="Root output directory for YOLO dataset",
    )
    parser.add_argument(
        "--img-size",
        type=int,
        default=416,
        help="Model input resolution (default: 416 for 416x416)",
    )
    parser.add_argument(
        "--val-split",
        type=float,
        default=0.20,
        help="Validation split ratio (default: 0.20 = 20%% val)",
    )
    parser.add_argument(
        "--max-videos",
        type=int,
        default=None,
        help="Maximum videos to process (useful for quick verification)",
    )
    parser.add_argument(
        "--ball-model",
        type=str,
        default="runs/detect/runs/detect/bowling_ball_detector/weights/best.pt",
        help="Path to pre-trained bowling ball detector weights",
    )
    parser.add_argument(
        "--visualize",
        action="store_true",
        help="Generate annotated visual overlays with class bounding boxes",
    )
    parser.add_argument(
        "--vis-count",
        type=int,
        default=100,
        help="Maximum visualization overlays to generate",
    )
    return parser.parse_args()


class MultiClassLandmarkDetector:
    """
    Algorithmic landmark annotator for bowling alleys.
    Extracts pin_rack, pins, foul_line, arrows, lane, and slide_foot.
    """

    def __init__(self, ball_model_path: Optional[str] = None):
        self.ball_model = None
        if ball_model_path and os.path.exists(ball_model_path):
            try:
                from ultralytics import YOLO
                self.ball_model = YOLO(ball_model_path)
                print(f"[MultiClassLandmarkDetector] Loaded bootstrap ball model: {ball_model_path}")
            except Exception as e:
                print(f"[MultiClassLandmarkDetector] Warning: could not load ball model: {e}")

    def detect_foul_line(self, gray: np.ndarray) -> Optional[Tuple[float, float, float, float]]:
        """
        Detects foul line (Class 3) using horizontal edge gradient between 48% and 62% of frame.
        Returns normalized (cx, cy, w, h).
        """
        h, w = gray.shape
        y_min, y_max = int(h * 0.48), int(h * 0.62)
        x_min, x_max = int(w * 0.15), int(w * 0.85)

        roi = gray[y_min:y_max, x_min:x_max]
        sobel_y = cv2.Sobel(roi, cv2.CV_64F, 0, 1, ksize=3)
        mean_grad = np.mean(np.abs(sobel_y), axis=1)

        if len(mean_grad) == 0:
            return None

        best_rel_y = int(np.argmax(mean_grad))
        foul_y = y_min + best_rel_y

        # Bounding box enclosing the horizontal black foul strip across lane
        box_w = 0.58
        box_h = 0.012
        cx = 0.50
        cy = foul_y / float(h)
        return (cx, cy, box_w, box_h)

    def detect_pin_rack_and_pins(
        self, gray: np.ndarray, is_pre_impact: bool = True
    ) -> Tuple[Optional[Tuple[float, float, float, float]], List[Tuple[float, float, float, float]]]:
        """
        Detects pin_rack (Class 1) and individual pins (Class 2) at the 60-ft pin deck.
        Returns (pin_rack_box, list_of_pin_boxes) in normalized (cx, cy, w, h).
        """
        h, w = gray.shape
        y_min, y_max = int(h * 0.34), int(h * 0.44)
        x_min, x_max = int(w * 0.32), int(w * 0.68)

        roi = gray[y_min:y_max, x_min:x_max]
        # Pins are bright white in contrast to the lane surface
        _, thresh = cv2.threshold(roi, 160, 255, cv2.THRESH_BINARY)

        contours, _ = cv2.findContours(thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

        pins = []
        for c in contours:
            area = cv2.contourArea(c)
            if area < 10:
                continue
            bx, by, bw, bh = cv2.boundingRect(c)
            # Filter reasonable pin dimensions
            if bh >= 4 and bw >= 2 and (bh / max(1, bw)) <= 5.0:
                abs_x1 = x_min + bx
                abs_y1 = y_min + by
                abs_x2 = abs_x1 + bw
                abs_y2 = abs_y1 + bh
                norm_cx = (abs_x1 + abs_x2) / 2.0 / w
                norm_cy = (abs_y1 + abs_y2) / 2.0 / h
                norm_w = bw / float(w)
                norm_h = bh / float(h)
                pins.append((norm_cx, norm_cy, norm_w, norm_h))

        pin_rack_box = None
        # pin_rack is only present pre-impact when pins form the triangle
        if is_pre_impact and len(pins) >= 3:
            rack_x1 = min(p[0] - p[2] / 2.0 for p in pins)
            rack_y1 = min(p[1] - p[3] / 2.0 for p in pins)
            rack_x2 = max(p[0] + p[2] / 2.0 for p in pins)
            rack_y2 = max(p[1] + p[3] / 2.0 for p in pins)

            rack_cx = (rack_x1 + rack_x2) / 2.0
            rack_cy = (rack_y1 + rack_y2) / 2.0
            rack_w = max(0.12, rack_x2 - rack_x1 + 0.01)
            rack_h = max(0.04, rack_y2 - rack_y1 + 0.01)
            pin_rack_box = (rack_cx, rack_cy, rack_w, rack_h)

        return pin_rack_box, pins

    def detect_arrows(
        self, foul_cy: float, pin_deck_cy: float = 0.38
    ) -> Tuple[float, float, float, float]:
        """
        Derives arrows bounding box (Class 4) at 15 ft via projective interpolation.
        Returns normalized (cx, cy, w, h).
        """
        # Under bowling perspective, 15 ft is ~48% of the distance from foul line to pin deck
        arrows_cy = foul_cy - (foul_cy - pin_deck_cy) * 0.48
        arrows_w = 0.46
        arrows_h = 0.035
        arrows_cx = 0.50
        return (arrows_cx, arrows_cy, arrows_w, arrows_h)

    def detect_lane(
        self, foul_cy: float, pin_deck_cy: float = 0.38
    ) -> Tuple[float, float, float, float]:
        """
        Detects active lane surface (Class 5) between foul line and pin deck.
        Returns normalized (cx, cy, w, h).
        """
        lane_cy = (foul_cy + pin_deck_cy) / 2.0
        lane_h = abs(foul_cy - pin_deck_cy)
        lane_w = 0.54
        lane_cx = 0.50
        return (lane_cx, lane_cy, lane_w, lane_h)

    def detect_slide_foot(
        self,
        curr_gray: np.ndarray,
        prev_gray: Optional[np.ndarray],
        foul_cy: float,
    ) -> Optional[Tuple[float, float, float, float]]:
        """
        Detects bowler's slide foot (Class 6) in the approach area right at/behind foul line.
        Returns normalized (cx, cy, w, h).
        """
        if prev_gray is None:
            return None

        h, w = curr_gray.shape
        diff = cv2.absdiff(curr_gray, prev_gray)

        # Region of interest: Approach zone behind foul line (y in [foul_y, foul_y + 0.18])
        y1 = int(h * foul_cy)
        y2 = min(h - 1, int(h * (foul_cy + 0.18)))
        x1 = int(w * 0.20)
        x2 = int(w * 0.80)

        roi_diff = diff[y1:y2, x1:x2]
        _, thresh = cv2.threshold(roi_diff, 18, 255, cv2.THRESH_BINARY)
        contours, _ = cv2.findContours(thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

        best_foot = None
        max_area = 0.0

        for c in contours:
            area = cv2.contourArea(c)
            if area > 120 and area > max_area:
                bx, by, bw, bh = cv2.boundingRect(c)
                # Shoe aspect ratio check
                if bw < int(w * 0.35) and bh < int(h * 0.25):
                    max_area = area
                    abs_cx = (x1 + bx + bw / 2.0) / float(w)
                    abs_cy = (y1 + by + bh / 2.0) / float(h)
                    norm_w = bw / float(w)
                    norm_h = bh / float(h)
                    best_foot = (abs_cx, abs_cy, norm_w, norm_h)

        return best_foot

    def detect_ball(
        self,
        curr_gray: np.ndarray,
        prev_gray: Optional[np.ndarray],
        foul_cy: float,
        pin_deck_cy: float = 0.38,
    ) -> Optional[Tuple[float, float, float, float]]:
        """
        Detects bowling ball (Class 0) in motion down the lane.
        Uses bootstrap YOLO model if available, falling back to motion differencing.
        Returns normalized (cx, cy, w, h).
        """
        # 1. Try bootstrap ML detector first
        if self.ball_model is not None:
            try:
                # Resize to 416x416 grayscale merged to 3-channel for YOLO inference
                resized = cv2.resize(curr_gray, (416, 416))
                merged = cv2.merge([resized, resized, resized])
                preds = self.ball_model.predict(merged, conf=0.10, verbose=False)
                if len(preds) > 0 and len(preds[0].boxes) > 0:
                    box = preds[0].boxes.xywhn[0].cpu().numpy().tolist()
                    return (box[0], box[1], box[2], box[3])
            except Exception:
                pass

        # 2. Fallback to temporal differencing + circularity on lane surface
        if prev_gray is None:
            return None

        h, w = curr_gray.shape
        diff = cv2.absdiff(curr_gray, prev_gray)

        y1 = int(h * pin_deck_cy)
        y2 = int(h * (foul_cy + 0.05))
        x1 = int(w * 0.20)
        x2 = int(w * 0.80)

        roi_diff = diff[y1:y2, x1:x2]
        _, thresh = cv2.threshold(roi_diff, 18, 255, cv2.THRESH_BINARY)
        contours, _ = cv2.findContours(thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

        best_ball = None
        best_circularity = 0.0

        for c in contours:
            area = cv2.contourArea(c)
            if 20 < area < 2500:
                perimeter = cv2.arcLength(c, True)
                if perimeter > 0:
                    circularity = 4 * math.pi * (area / (perimeter * perimeter))
                    if circularity > 0.45 and circularity > best_circularity:
                        bx, by, bw, bh = cv2.boundingRect(c)
                        best_circularity = circularity
                        norm_cx = (x1 + bx + bw / 2.0) / float(w)
                        norm_cy = (y1 + by + bh / 2.0) / float(h)
                        norm_w = bw / float(w)
                        norm_h = bh / float(h)
                        best_ball = (norm_cx, norm_cy, norm_w, norm_h)

        return best_ball


def find_delivery_windows(
    video_path: str,
    stride: int = 5,
    min_energy: float = 18.0,
) -> List[Tuple[int, int]]:
    """
    Scans video and extracts active bowling delivery frame intervals (start_frame, end_frame).
    """
    cap = cv2.VideoCapture(video_path)
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    if total_frames <= 0:
        cap.release()
        return []

    prev_roi = None
    active_runs = []
    in_delivery = False
    run_start = 0

    for f_idx in range(0, total_frames, stride):
        cap.set(cv2.CAP_PROP_POS_FRAMES, f_idx)
        ret, frame = cap.read()
        if not ret:
            break

        h, w = frame.shape[:2]
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        # Measure motion strictly on lane surface
        lane_roi = gray[int(h * 0.37) : int(h * 0.58), int(w * 0.20) : int(w * 0.80)]

        if prev_roi is not None:
            energy = float(np.mean(cv2.absdiff(lane_roi, prev_roi)))
            if energy >= min_energy:
                if not in_delivery:
                    in_delivery = True
                    run_start = max(0, f_idx - 15)
            else:
                if in_delivery:
                    in_delivery = False
                    run_end = min(total_frames - 1, f_idx + 15)
                    if (run_end - run_start) >= 20:
                        active_runs.append((run_start, run_end))

        prev_roi = lane_roi

    if in_delivery:
        active_runs.append((run_start, total_frames - 1))

    cap.release()
    return active_runs


def sample_delivery_phases(
    start_f: int, end_f: int
) -> Dict[str, List[int]]:
    """
    Partitions a delivery window into 4 distinct kinematic phases:
      Phase A (Stance & Approach): 2 frames
      Phase B (Slide & Release): 4 frames
      Phase C (Mid-Lane Roll): 10 frames
      Phase D (Impact & Scatter): 5 frames
    """
    duration = end_f - start_f
    if duration < 10:
        return {}

    # Fractional division of bowling delivery
    phases = {}

    def linspace_frames(f1, f2, count):
        if f2 <= f1:
            return [int(f1)]
        return [int(f1 + (f2 - f1) * (i / max(1, count - 1))) for i in range(count)]

    f_stance_end = start_f + int(duration * 0.22)
    f_release_end = start_f + int(duration * 0.42)
    f_mid_end = start_f + int(duration * 0.78)

    phases["A"] = linspace_frames(start_f, f_stance_end, 2)
    phases["B"] = linspace_frames(f_stance_end, f_release_end, 4)
    phases["C"] = linspace_frames(f_release_end, f_mid_end, 10)
    phases["D"] = linspace_frames(f_mid_end, end_f, 5)

    return phases


def draw_visual_overlay(
    img_bgr: np.ndarray,
    labels: List[Tuple[int, float, float, float, float]],
) -> np.ndarray:
    """
    Draws colored bounding boxes and class labels on image for visual quality review.
    """
    vis = img_bgr.copy()
    h, w = vis.shape[:2]

    for cid, cx, cy, bw, bh in labels:
        x1 = int((cx - bw / 2.0) * w)
        y1 = int((cy - bh / 2.0) * h)
        x2 = int((cx + bw / 2.0) * w)
        y2 = int((cy + bh / 2.0) * h)

        color = CLASS_COLORS[cid % len(CLASS_COLORS)]
        cv2.rectangle(vis, (x1, y1), (x2, y2), color, 2)

        label_txt = f"{CLASS_NAMES[cid]}"
        cv2.putText(
            vis,
            label_txt,
            (x1, max(14, y1 - 4)),
            cv2.FONT_HERSHEY_SIMPLEX,
            0.45,
            color,
            1,
            cv2.LINE_AA,
        )

    return vis


def main():
    args = parse_args()
    print("=" * 70)
    print("Bowling Lab Track - Multi-Class Dataset Mining & Annotation")
    print("=" * 70)

    # Search for all videos
    video_paths = sorted(
        glob.glob(os.path.join(args.input_dir, "**/*.mp4"), recursive=True)
        + glob.glob(os.path.join(args.input_dir, "**/*.mov"), recursive=True)
    )
    if not video_paths:
        print(f"Error: No video files found in {args.input_dir}")
        sys.exit(1)

    if args.max_videos:
        video_paths = video_paths[: args.max_videos]

    print(f"Found {len(video_paths)} videos to process.")
    print(f"Target Resolution: {args.img_size}x{args.img_size}")
    print(f"Output Directory: {args.output_dir}")

    # Prepare output directories
    out_train_img = os.path.join(args.output_dir, "train", "images")
    out_train_lbl = os.path.join(args.output_dir, "train", "labels")
    out_val_img = os.path.join(args.output_dir, "val", "images")
    out_val_lbl = os.path.join(args.output_dir, "val", "labels")
    out_vis_dir = os.path.join(args.output_dir, "visualizations")

    for d in [out_train_img, out_train_lbl, out_val_img, out_val_lbl, out_vis_dir]:
        os.makedirs(d, exist_ok=True)

    # Initialize landmark detector
    landmark_detector = MultiClassLandmarkDetector(args.ball_model)

    total_images = 0
    total_annotations = {i: 0 for i in range(len(CLASS_NAMES))}
    vis_counter = 0

    for vid_idx, vid_path in enumerate(video_paths):
        vname = Path(vid_path).stem
        print(f"\n[{vid_idx + 1}/{len(video_paths)}] Processing: {vname}...")

        delivery_runs = find_delivery_windows(vid_path)
        print(f"  Found {len(delivery_runs)} active delivery windows.")

        cap = cv2.VideoCapture(vid_path)
        prev_gray_frame = None

        for run_idx, (start_f, end_f) in enumerate(delivery_runs):
            phases = sample_delivery_phases(start_f, end_f)

            for phase_id, frame_indices in phases.items():
                is_pre_impact = phase_id in ["A", "B", "C"]
                is_release = phase_id == "B"
                is_rolling = phase_id in ["B", "C", "D"]

                for f_num in frame_indices:
                    cap.set(cv2.CAP_PROP_POS_FRAMES, f_num)
                    ret, frame = cap.read()
                    if not ret:
                        continue

                    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
                    labels = []

                    # 1. Foul Line (Class 3)
                    foul_box = landmark_detector.detect_foul_line(gray)
                    foul_cy = foul_box[1] if foul_box else 0.54
                    if foul_box:
                        labels.append((3, foul_box[0], foul_box[1], foul_box[2], foul_box[3]))

                    # 2. Pin Rack (Class 1) & Pins (Class 2)
                    pin_rack_box, pins = landmark_detector.detect_pin_rack_and_pins(
                        gray, is_pre_impact=is_pre_impact
                    )
                    if pin_rack_box:
                        labels.append((1, pin_rack_box[0], pin_rack_box[1], pin_rack_box[2], pin_rack_box[3]))
                    for p in pins:
                        labels.append((2, p[0], p[1], p[2], p[3]))

                    # 3. Arrows (Class 4)
                    arrows_box = landmark_detector.detect_arrows(foul_cy)
                    labels.append((4, arrows_box[0], arrows_box[1], arrows_box[2], arrows_box[3]))

                    # 4. Lane Surface (Class 5)
                    lane_box = landmark_detector.detect_lane(foul_cy)
                    labels.append((5, lane_box[0], lane_box[1], lane_box[2], lane_box[3]))

                    # 5. Slide Foot (Class 6) - only during release phase
                    if is_release and prev_gray_frame is not None:
                        foot_box = landmark_detector.detect_slide_foot(gray, prev_gray_frame, foul_cy)
                        if foot_box:
                            labels.append((6, foot_box[0], foot_box[1], foot_box[2], foot_box[3]))

                    # 6. Bowling Ball (Class 0) - during active roll
                    if is_rolling:
                        ball_box = landmark_detector.detect_ball(gray, prev_gray_frame, foul_cy)
                        if ball_box:
                            labels.append((0, ball_box[0], ball_box[1], ball_box[2], ball_box[3]))

                    prev_gray_frame = gray.copy()

                    if not labels:
                        continue

                    # Resize to target resolution (416x416)
                    resized_bgr = cv2.resize(frame, (args.img_size, args.img_size))

                    # Train vs Validation split
                    is_val = random.random() < args.val_split
                    target_img_dir = out_val_img if is_val else out_train_img
                    target_lbl_dir = out_val_lbl if is_val else out_train_lbl

                    base_filename = f"{vname}_d{run_idx}_p{phase_id}_f{f_num:06d}"
                    img_out_path = os.path.join(target_img_dir, f"{base_filename}.jpg")
                    lbl_out_path = os.path.join(target_lbl_dir, f"{base_filename}.txt")

                    # Save resized image
                    cv2.imwrite(img_out_path, resized_bgr)

                    # Save YOLO annotations
                    with open(lbl_out_path, "w") as fp:
                        for cid, cx, cy, bw, bh in labels:
                            cx = max(0.001, min(0.999, cx))
                            cy = max(0.001, min(0.999, cy))
                            bw = max(0.001, min(0.999, bw))
                            bh = max(0.001, min(0.999, bh))
                            fp.write(f"{cid} {cx:.6f} {cy:.6f} {bw:.6f} {bh:.6f}\n")
                            total_annotations[cid] += 1

                    total_images += 1

                    # Optional visual overlay export
                    if args.visualize and vis_counter < args.vis_count:
                        vis_img = draw_visual_overlay(resized_bgr, labels)
                        vis_path = os.path.join(out_vis_dir, f"{base_filename}_vis.jpg")
                        cv2.imwrite(vis_path, vis_img)
                        vis_counter += 1

        cap.release()

    # Generate data.yaml
    data_yaml_path = os.path.join(args.output_dir, "data.yaml")
    with open(data_yaml_path, "w") as fp:
        fp.write(f"path: {os.path.abspath(args.output_dir)}\n")
        fp.write("train: train/images\n")
        fp.write("val: val/images\n")
        fp.write(f"nc: {len(CLASS_NAMES)}\n")
        fp.write("names:\n")
        for i, name in enumerate(CLASS_NAMES):
            fp.write(f"  {i}: {name}\n")

    print("\n" + "=" * 70)
    print("Multi-Class Dataset Mining Complete")
    print("=" * 70)
    print(f"Total Images Generated: {total_images}")
    print("Class Annotation Breakdown:")
    for cid, name in enumerate(CLASS_NAMES):
        print(f"  {cid}: {name:15s} -> {total_annotations[cid]:6d} bounding boxes")
    print(f"YOLO data.yaml: {data_yaml_path}")
    if args.visualize:
        print(f"Visual QA Overlays: {out_vis_dir} ({vis_counter} images)")


if __name__ == "__main__":
    main()
