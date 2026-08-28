import React, { useEffect, useState } from 'react';
import { 
  ShieldCheck, 
  X, 
  CheckCircle2, 
  Printer 
} from 'lucide-react';


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
  const [activeTab, setActiveTab] = useState<'all' | 'liability' | 'safesport' | 'media' | 'cancellation'>('all');

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
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-950/85 backdrop-blur-sm flex items-start sm:items-center justify-center p-2 sm:p-4 md:p-6 animate-in fade-in duration-150">
      <div className="relative w-full max-w-4xl my-auto max-h-[92vh] flex flex-col bg-white rounded-3xl shadow-2xl border-2 border-slate-300 overflow-hidden">
        
        {/* Modal Header */}
        <div className="bg-[#00205b] text-white p-6 sm:p-8 border-b-4 border-[#c8102e] flex items-center justify-between relative shrink-0">
          <div className="space-y-1">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/10 text-[#c39d5e] border border-white/20 text-xs font-bold uppercase tracking-wider">
              <ShieldCheck className="w-3.5 h-3.5 text-[#c8102e]" />
              <span>Official Athlete Agreements & Policies</span>
            </div>
            <h3 className="font-display text-2xl sm:text-3xl font-black uppercase tracking-tight text-white">
              Coach's Eye Bowling Lab
            </h3>
            <p className="text-xs sm:text-sm text-slate-200 font-body">
              Alfredo Quilarquez • Bowlero West Covina • USBC Level 1 & SafeSport Cleared
            </p>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handlePrint}
              aria-label="Print legal forms"
              className="hidden sm:flex w-10 h-10 rounded-xl bg-white/10 hover:bg-white/20 border border-white/25 text-white items-center justify-center transition-all cursor-pointer shadow-xs"
              title="Print All 4 Forms"
            >
              <Printer className="w-4 h-4" />
            </button>

            <button
              onClick={onClose}
              aria-label="Close modal"
              className="w-10 h-10 rounded-full bg-white/20 hover:bg-[#c8102e] border border-white/30 text-white flex items-center justify-center shadow-lg hover:scale-105 transition-all cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Navigation Filter Pills */}
        <div className="bg-slate-100 border-b border-slate-200 px-4 sm:px-8 py-3 flex items-center gap-2 overflow-x-auto shrink-0 scrollbar-none">
          <button
            onClick={() => setActiveTab('all')}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
              activeTab === 'all' 
                ? 'bg-[#00205b] text-white shadow-xs' 
                : 'text-slate-600 hover:bg-slate-200'
            }`}
          >
            All 4 Documents
          </button>
          <button
            onClick={() => setActiveTab('liability')}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
              activeTab === 'liability' 
                ? 'bg-[#00205b] text-white shadow-xs' 
                : 'text-slate-600 hover:bg-slate-200'
            }`}
          >
            1. Liability Release
          </button>
          <button
            onClick={() => setActiveTab('safesport')}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
              activeTab === 'safesport' 
                ? 'bg-[#00205b] text-white shadow-xs' 
                : 'text-slate-600 hover:bg-slate-200'
            }`}
          >
            2. Minor & SafeSport
          </button>
          <button
            onClick={() => setActiveTab('media')}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
              activeTab === 'media' 
                ? 'bg-[#00205b] text-white shadow-xs' 
                : 'text-slate-600 hover:bg-slate-200'
            }`}
          >
            3. Video & Media
          </button>
          <button
            onClick={() => setActiveTab('cancellation')}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
              activeTab === 'cancellation' 
                ? 'bg-[#00205b] text-white shadow-xs' 
                : 'text-slate-600 hover:bg-slate-200'
            }`}
          >
            4. Package Terms & Cancel
          </button>
        </div>

        {/* Scrollable Content Container */}
        <div className="overflow-y-auto p-6 sm:p-10 space-y-10 text-slate-700 text-xs sm:text-sm leading-relaxed font-body">
          
          {/* NOTICE ALERT */}
          <div className="p-4 bg-blue-50 border border-blue-200 rounded-2xl flex items-start gap-3 text-[#00205b] text-xs font-semibold">
            <ShieldCheck className="w-5 h-5 text-[#c8102e] shrink-0 mt-0.5" />
            <div>
              <strong>ATHLETE AGREEMENT REPOSITORY:</strong> These forms are fully customized for <strong>Coach's Eye Bowling Lab</strong> and coaching services conducted on-lane at <strong>Bowlero West Covina</strong>.
            </div>
          </div>

          {/* FORM 1: Assumption of Risk & Liability Release */}
          {(activeTab === 'all' || activeTab === 'liability') && (
            <div className="card-usbc p-6 sm:p-8 border-2 border-slate-200 rounded-2xl space-y-4 bg-white shadow-xs">
              <div className="flex items-center justify-between border-b border-slate-200 pb-3">
                <div className="flex items-center gap-2">
                  <span className="w-7 h-7 rounded-lg bg-[#00205b] text-white flex items-center justify-center font-black text-xs">
                    1
                  </span>
                  <div>
                    <div className="text-[10px] font-bold text-[#c8102e] uppercase tracking-wider">Document 01</div>
                    <h4 className="font-display text-lg sm:text-xl font-black text-[#00205b] uppercase">
                      Assumption of Risk & Liability Release
                    </h4>
                  </div>
                </div>
                <span className="text-[10px] font-bold uppercase bg-slate-100 text-slate-600 px-2.5 py-1 rounded">
                  Legal Waiver
                </span>
              </div>

              <div className="space-y-3.5 text-xs text-slate-700">
                <div>
                  <strong className="text-slate-900 uppercase block mb-1">Inherent Risks:</strong>
                  <p>
                    I acknowledge that participating in bowling instruction with Alfredo Quilarquez at Bowlero West Covina involves inherent physical risks. These risks include, but are not limited to, slip-and-fall hazards on the approach, repetitive motion strains (including wrist, shoulder, and knee), and injuries resulting from dropped bowling equipment.
                  </p>
                </div>

                <div>
                  <strong className="text-slate-900 uppercase block mb-1">Waiver of Claims:</strong>
                  <p>
                    I explicitly release Alfredo Quilarquez and Coach's Eye Bowling Lab from any legal liability, claims, or demands regarding bodily injury or property damage sustained during on-lane physical training. I voluntarily assume all physical risks inherent to the sport.
                  </p>
                </div>

                <div>
                  <strong className="text-slate-900 uppercase block mb-1">Medical Fitness:</strong>
                  <p>
                    I verify that I am physically capable of participating in mechanical drills, 60-Minute Tune-Ups, Multi-Week Camps, and On-Lane Strategy Sessions, and have no undisclosed medical conditions that would prevent safe participation.
                  </p>
                </div>
              </div>

              <div className="pt-3 border-t border-slate-100 flex flex-wrap items-center justify-between gap-2 text-[11px] text-slate-500 italic">
                <span>(Digital Signature & Date recorded upon Diagnostic Submission)</span>
                <span className="font-bold text-[#00205b] not-italic">Coach's Eye Bowling Lab</span>
              </div>
            </div>
          )}

          {/* FORM 2: Minor Consent & SafeSport Acknowledgment */}
          {(activeTab === 'all' || activeTab === 'safesport') && (
            <div className="card-usbc p-6 sm:p-8 border-2 border-slate-200 rounded-2xl space-y-4 bg-white shadow-xs">
              <div className="flex items-center justify-between border-b border-slate-200 pb-3">
                <div className="flex items-center gap-2">
                  <span className="w-7 h-7 rounded-lg bg-[#c8102e] text-white flex items-center justify-center font-black text-xs">
                    2
                  </span>
                  <div>
                    <div className="text-[10px] font-bold text-[#c8102e] uppercase tracking-wider">Document 02</div>
                    <h4 className="font-display text-lg sm:text-xl font-black text-[#00205b] uppercase">
                      Minor Consent & SafeSport Acknowledgment
                    </h4>
                  </div>
                </div>
                <span className="text-[10px] font-bold uppercase bg-emerald-50 text-emerald-700 border border-emerald-200 px-2.5 py-1 rounded">
                  USBC SafeSport / RVP
                </span>
              </div>

              <div className="space-y-3.5 text-xs text-slate-700">
                <div>
                  <strong className="text-slate-900 uppercase block mb-1">Parental Authorization:</strong>
                  <p>
                    As the legal guardian of the participating youth bowler under 18 years of age, I hereby consent to their participation in the coaching programs provided by Coach's Eye Bowling Lab.
                  </p>
                </div>

                <div>
                  <strong className="text-slate-900 uppercase block mb-1">SafeSport Compliance:</strong>
                  <p>
                    I acknowledge and understand that Coach Alfredo Quilarquez is a USBC SafeSport and Registered Volunteer Program (RVP) cleared coach. I understand that all youth instruction will strictly adhere to open-observation protocols to ensure a safe environment. No private, unmonitored closed-room sessions will take place.
                  </p>
                </div>

                <div>
                  <strong className="text-slate-900 uppercase block mb-1">Medical Emergency:</strong>
                  <p>
                    In the event of an emergency where I cannot be reached, I grant permission for Coach's Eye Bowling Lab staff to secure necessary emergency medical treatment for the minor at my expense.
                  </p>
                </div>
              </div>

              <div className="pt-3 border-t border-slate-100 flex flex-wrap items-center justify-between gap-2 text-[11px] text-slate-500 italic">
                <span>(Parent/Guardian Signature Line and Date required for youth participants)</span>
                <span className="font-bold text-[#00205b] not-italic">USBC RVP Verified</span>
              </div>
            </div>
          )}

          {/* FORM 3: Biomechanics & Video Media Release */}
          {(activeTab === 'all' || activeTab === 'media') && (
            <div className="card-usbc p-6 sm:p-8 border-2 border-slate-200 rounded-2xl space-y-4 bg-white shadow-xs">
              <div className="flex items-center justify-between border-b border-slate-200 pb-3">
                <div className="flex items-center gap-2">
                  <span className="w-7 h-7 rounded-lg bg-[#00205b] text-white flex items-center justify-center font-black text-xs">
                    3
                  </span>
                  <div>
                    <div className="text-[10px] font-bold text-[#c8102e] uppercase tracking-wider">Document 03</div>
                    <h4 className="font-display text-lg sm:text-xl font-black text-[#00205b] uppercase">
                      Biomechanics & Video Media Release
                    </h4>
                  </div>
                </div>
                <span className="text-[10px] font-bold uppercase bg-blue-50 text-[#00205b] border border-blue-200 px-2.5 py-1 rounded">
                  240 FPS Capture
                </span>
              </div>

              <div className="space-y-3.5 text-xs text-slate-700">
                <div>
                  <strong className="text-slate-900 uppercase block mb-1">Instructional Video Capture:</strong>
                  <p>
                    I consent to being filmed and photographed during my sessions at Coach's Eye Bowling Lab. I understand that high-speed video analysis and biomechanical breakdowns are central to the instruction and are used to improve my physical timing, leverage, and lane play execution.
                  </p>
                </div>

                <div>
                  <strong className="text-slate-900 uppercase block mb-1">Marketing & Social Media Release Options:</strong>
                  <div className="space-y-2 mt-2 bg-slate-50 p-4 rounded-xl border border-slate-200">
                    <div className="flex items-start gap-2">
                      <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                      <span><strong>Option A (Promotional):</strong> I GRANT Coach's Eye Bowling Lab permission to use anonymized or credited footage of my physical game on social media channels, promotional materials, and the company website.</span>
                    </div>
                    <div className="flex items-start gap-2">
                      <CheckCircle2 className="w-4 h-4 text-slate-400 shrink-0 mt-0.5" />
                      <span><strong>Option B (Private Only):</strong> I DO NOT GRANT permission for my footage to be used outside of my personal instructional analysis.</span>
                    </div>
                  </div>
                </div>

                <div>
                  <strong className="text-slate-900 uppercase block mb-1">Data Usage:</strong>
                  <p>
                    I understand that my video files and performance data will be stored securely and used in accordance with applicable data privacy guidelines, strictly for the advancement of my bowling performance.
                  </p>
                </div>
              </div>

              <div className="pt-3 border-t border-slate-100 flex flex-wrap items-center justify-between gap-2 text-[11px] text-slate-500 italic">
                <span>(Selected via Diagnostic Form: Default or Custom Media Preference)</span>
                <span className="font-bold text-[#00205b] not-italic">Coach's Eye Media Protocol</span>
              </div>
            </div>
          )}

          {/* FORM 4: Package Terms & Cancellation Policy */}
          {(activeTab === 'all' || activeTab === 'cancellation') && (
            <div className="card-usbc p-6 sm:p-8 border-2 border-slate-200 rounded-2xl space-y-4 bg-white shadow-xs">
              <div className="flex items-center justify-between border-b border-slate-200 pb-3">
                <div className="flex items-center gap-2">
                  <span className="w-7 h-7 rounded-lg bg-[#c8102e] text-white flex items-center justify-center font-black text-xs">
                    4
                  </span>
                  <div>
                    <div className="text-[10px] font-bold text-[#c8102e] uppercase tracking-wider">Document 04</div>
                    <h4 className="font-display text-lg sm:text-xl font-black text-[#00205b] uppercase">
                      Package Terms & Cancellation Policy
                    </h4>
                  </div>
                </div>
                <span className="text-[10px] font-bold uppercase bg-amber-50 text-amber-800 border border-amber-200 px-2.5 py-1 rounded">
                  24-Hour Policy
                </span>
              </div>

              <div className="space-y-3.5 text-xs text-slate-700">
                <div>
                  <strong className="text-slate-900 uppercase block mb-1">Rescheduling & Cancellation Window:</strong>
                  <p>
                    I agree to provide a minimum of 24 hours' notice to cancel or reschedule any booked session, including the 60-Minute Tune-Up or On-Lane Strategy Session. Failure to provide 24 hours' notice will result in forfeiting the session fee.
                  </p>
                </div>

                <div>
                  <strong className="text-slate-900 uppercase block mb-1">Package Validity & Expiration:</strong>
                  <p>
                    I understand that bundled services, such as the 5-Session Development Package or Multi-Week Camps, represent a commitment to progressive improvement. All prepaid packages must be completed within 6 months from the date of purchase.
                  </p>
                </div>

                <div>
                  <strong className="text-slate-900 uppercase block mb-1">Refunds:</strong>
                  <p>
                    I acknowledge that coaching fees and deposits are non-refundable once a training block or single session has commenced. Exceptions are made solely at the discretion of Coach's Eye Bowling Lab management.
                  </p>
                </div>
              </div>

              <div className="pt-3 border-t border-slate-100 flex flex-wrap items-center justify-between gap-2 text-[11px] text-slate-500 italic">
                <span>(Agreed to prior to booking confirmation at Bowlero West Covina)</span>
                <span className="font-bold text-[#00205b] not-italic">Coach's Eye Terms & Policy</span>
              </div>
            </div>
          )}

        </div>

        {/* Modal Action Footer */}
        <div className="bg-slate-50 p-5 sm:p-6 border-t border-slate-200 flex flex-col sm:flex-row items-center justify-between gap-4 shrink-0">
          <div className="flex items-center gap-2 text-xs text-slate-600">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>All 4 agreements are stored with your digital bowler record.</span>
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
                Accept All 4 Agreements
              </button>
            )}
          </div>
        </div>

      </div>
    </div>
  );
};
