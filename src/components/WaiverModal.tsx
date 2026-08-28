import React, { useEffect } from 'react';
import { ShieldCheck, X, FileText, CheckCircle2, AlertTriangle, Video, HeartPulse, Printer } from 'lucide-react';

interface WaiverModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAccept?: () => void;
}

export const WaiverModal: React.FC<WaiverModalProps> = ({
  isOpen,
  onClose,
  onAccept
}) => {
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = 'unset';
    }
    return () => {
      document.body.style.overflow = 'unset';
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-950/80 backdrop-blur-sm flex items-start sm:items-center justify-center p-3 sm:p-6 animate-in fade-in duration-150">
      <div className="relative w-full max-w-4xl my-auto max-h-[92vh] flex flex-col bg-white rounded-3xl shadow-2xl border-2 border-slate-300 overflow-hidden">
        
        {/* Modal Header */}
        <div className="bg-[#00205b] text-white p-6 sm:p-8 border-b-4 border-[#c8102e] flex items-center justify-between relative shrink-0">
          <div className="space-y-1">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/10 text-[#c39d5e] border border-white/20 text-xs font-bold uppercase tracking-wider">
              <ShieldCheck className="w-3.5 h-3.5 text-[#c8102e]" />
              <span>Official Legal Agreement</span>
            </div>
            <h3 className="font-display text-2xl sm:text-3xl font-black uppercase tracking-tight text-white">
              Athletic Liability Waiver & Video Consent
            </h3>
            <p className="text-xs sm:text-sm text-slate-200 font-body">
              Coach's Eye Bowling Lab • Alfredo Quilarquez • Bowlero West Covina
            </p>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handlePrint}
              aria-label="Print waiver"
              className="hidden sm:flex w-9 h-9 rounded-xl bg-white/10 hover:bg-white/20 border border-white/25 text-white items-center justify-center transition-all cursor-pointer"
              title="Print Waiver"
            >
              <Printer className="w-4 h-4" />
            </button>

            <button
              onClick={onClose}
              aria-label="Close waiver modal"
              className="w-10 h-10 rounded-full bg-white/20 hover:bg-[#c8102e] border border-white/30 text-white flex items-center justify-center shadow-lg hover:scale-105 transition-all cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Scrollable Waiver Legal Text */}
        <div className="overflow-y-auto p-6 sm:p-10 space-y-8 text-slate-700 text-xs sm:text-sm leading-relaxed font-body">
          
          <div className="p-4 bg-amber-50 border border-amber-200 rounded-2xl flex items-start gap-3 text-amber-900 text-xs font-semibold">
            <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
            <div>
              <strong>IMPORTANT LEGAL NOTICE:</strong> Please read this document carefully before participating in any on-lane bowling coaching, biomechanical video capture, or training clinics. By signing/accepting, you acknowledge physical risks and waive certain legal rights.
            </div>
          </div>

          {/* Section 1: Inherent Risks */}
          <div className="space-y-3">
            <div className="flex items-center gap-2 text-sm sm:text-base font-bold text-[#00205b] uppercase font-display">
              <FileText className="w-4 h-4 text-[#c8102e]" />
              <span>1. Assumption of Inherent Athletic Risks</span>
            </div>
            <p>
              I understand that tenpin bowling is an athletic activity involving physical exertion, dynamic sliding footwork, rotational spinal leverage, repetitive joint movement, and the handling of weighted bowling equipment (6 to 16 pounds). I recognize that participation in bowling lessons and on-lane drills carries inherent risks of injury, including but not limited to:
            </p>
            <ul className="list-disc pl-5 space-y-1.5 text-xs text-slate-600">
              <li>Slips, trips, or falls due to approach surface conditions, friction variations, slide shoe sticking, or lane conditioning oil.</li>
              <li>Muscle strains, sprains, tendonitis, or joint stress involving the wrist, elbow, shoulder, spine, hip, knee, or ankle.</li>
              <li>Impact injuries from dropped bowling balls or mechanical ball-return equipment.</li>
              <li>Cardiovascular stress associated with athletic physical exertion.</li>
            </ul>
            <p className="font-semibold text-slate-900">
              I knowingly, freely, and voluntarily assume all such risks, both known and unknown, and assume full personal responsibility for my participation.
            </p>
          </div>

          {/* Section 2: Release of Liability */}
          <div className="space-y-3 border-t border-slate-200 pt-6">
            <div className="flex items-center gap-2 text-sm sm:text-base font-bold text-[#00205b] uppercase font-display">
              <ShieldCheck className="w-4 h-4 text-[#c8102e]" />
              <span>2. Release of Liability & Covenant Not to Sue</span>
            </div>
            <p>
              To the maximum extent permitted by law, I hereby release, waive, discharge, and hold harmless <strong>Alfredo Quilarquez</strong>, <strong>Coach's Eye Bowling Lab</strong>, <strong>Bowlero West Covina</strong>, and their respective owners, instructors, employees, contractors, and agents (collectively, "Released Parties") from any and all liability, claims, demands, actions, or rights of action arising out of any personal injury, disability, property damage, or loss sustained during or related to bowling instruction sessions, clinics, or facility presence.
            </p>
          </div>

          {/* Section 3: 240fps Biomechanical Video Consent */}
          <div className="space-y-3 border-t border-slate-200 pt-6">
            <div className="flex items-center gap-2 text-sm sm:text-base font-bold text-[#00205b] uppercase font-display">
              <Video className="w-4 h-4 text-[#c8102e]" />
              <span>3. Video Analysis & Media Consent</span>
            </div>
            <p>
              I acknowledge that high-speed multi-angle video recording (240fps slow-motion) is an essential component of Coach Alfredo's biomechanical coaching methodology. I grant permission for Coach's Eye Bowling Lab to record and analyze my physical approach, release, and ball trajectory strictly for technical instruction and progress tracking.
            </p>
            <p className="text-xs text-slate-600">
              <em>Note: Video clips and slow-motion telemetry review files are provided directly to the student for practice reference. Any promotional use of footage will require explicit student approval.</em>
            </p>
          </div>

          {/* Section 4: Medical Fitness & Emergency Authorization */}
          <div className="space-y-3 border-t border-slate-200 pt-6">
            <div className="flex items-center gap-2 text-sm sm:text-base font-bold text-[#00205b] uppercase font-display">
              <HeartPulse className="w-4 h-4 text-[#c8102e]" />
              <span>4. Medical Fitness & Emergency Authorization</span>
            </div>
            <p>
              I certify that I am physically capable of participating in athletic bowling instruction and have disclosed any pre-existing physical limitations or injuries in the Preliminary Diagnostic Evaluation. In the event of a medical emergency during a session, I authorize Coach Alfredo or facility personnel to secure emergency medical care if necessary.
            </p>
          </div>

          {/* Section 5: Minors & Parental Signature */}
          <div className="space-y-3 border-t border-slate-200 pt-6 bg-slate-50 p-5 rounded-2xl border border-slate-200">
            <div className="font-bold text-[#00205b] uppercase text-xs">
              5. Minor Participants (Under 18 Years of Age)
            </div>
            <p className="text-xs text-slate-600">
              If the participant is under 18 years old, a parent or legal guardian must review and accept this agreement on behalf of the minor, agreeing to all terms and liability releases stated herein.
            </p>
          </div>

        </div>

        {/* Modal Action Footer */}
        <div className="bg-slate-50 p-5 sm:p-6 border-t border-slate-200 flex flex-col sm:flex-row items-center justify-between gap-4 shrink-0">
          <div className="flex items-center gap-2 text-xs text-slate-600">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>Digital acknowledgment is recorded with your diagnostic baseline submission.</span>
          </div>

          <div className="flex items-center gap-3 w-full sm:w-auto">
            <button
              onClick={onClose}
              className="w-full sm:w-auto px-6 py-3.5 rounded-xl border border-slate-300 text-slate-700 hover:bg-slate-100 font-bold text-xs uppercase tracking-wider transition-all cursor-pointer"
            >
              Close
            </button>

            {onAccept && (
              <button
                onClick={() => {
                  onAccept();
                  onClose();
                }}
                className="w-full sm:w-auto bg-[#c8102e] hover:bg-[#a60d24] text-white font-bold text-xs uppercase tracking-wider px-7 py-3.5 rounded-xl shadow-md transition-all cursor-pointer border border-[#a60d24]"
              >
                I Accept Waiver & Terms
              </button>
            )}
          </div>
        </div>

      </div>
    </div>
  );
};
