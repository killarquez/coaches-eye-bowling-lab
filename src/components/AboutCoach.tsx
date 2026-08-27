import React from 'react';
import { 
  Video, 
  Target, 
  Compass, 
  CheckCircle2, 
  Calendar,
  MapPin,
  FileCheck
} from 'lucide-react';

interface AboutCoachProps {
  onBookClick: () => void;
  onOpenDiagnostic: () => void;
}

export const AboutCoach: React.FC<AboutCoachProps> = ({ onBookClick, onOpenDiagnostic }) => {
  return (
    <div className="space-y-24">
      
      {/* Bio Section Card with Generous Padding & Space */}
      <div className="card-usbc p-8 sm:p-12 lg:p-16 border border-slate-200 shadow-md">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-16 items-center">
          
          {/* Left Column: Visual Profile Card */}
          <div className="lg:col-span-5 text-center">
            <div className="bg-slate-50 p-8 rounded-3xl border border-slate-200 space-y-6">
              
              <div className="w-36 h-36 mx-auto rounded-3xl bg-[#0b1e3b] border-4 border-[#c8102e] flex items-center justify-center p-1 text-white shadow-xl overflow-hidden">
                <img
                  src="/logo-emblem.png"
                  alt="Coach's Eye Bowling Lab"
                  className="w-full h-full object-cover"
                />
              </div>


              <div className="space-y-1">
                <h3 className="font-display text-2xl sm:text-3xl font-black text-[#00205b] uppercase">
                  Alfredo Quilarquez
                </h3>
                <p className="text-xs font-bold text-[#c8102e] uppercase tracking-wider">
                  USBC Certified Level 1 Coach • PBA Member
                </p>
              </div>

              {/* Verified Badges Table with Aligned Rows */}
              <div className="space-y-2.5 text-xs font-medium text-slate-700 text-left bg-white p-5 rounded-2xl border border-slate-200 shadow-xs">
                <div className="flex justify-between items-center border-b border-slate-100 pb-2">
                  <span className="text-slate-500">Home Facility:</span>
                  <span className="font-bold text-[#00205b]">Bowlero West Covina</span>
                </div>
                <div className="flex justify-between items-center border-b border-slate-100 pb-2">
                  <span className="text-slate-500">USBC Status:</span>
                  <span className="font-bold text-emerald-700">Level 1 (Bronze Candidate)</span>
                </div>
                <div className="flex justify-between items-center border-b border-slate-100 pb-2">
                  <span className="text-slate-500">PBA Tour:</span>
                  <span className="font-bold text-[#00205b]">Active Regional Competitor</span>
                </div>
                <div className="flex justify-between items-center border-b border-slate-100 pb-2">
                  <span className="text-slate-500">Official High Series:</span>
                  <span className="font-bold text-[#c8102e]">730 (Sanctioned High Mark)</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-slate-500">Compliance:</span>
                  <span className="font-bold text-slate-900">SafeSport & RVP Cleared</span>
                </div>
              </div>

              <div className="pt-2 text-center text-xs font-semibold text-slate-500 flex items-center justify-center gap-2">
                <MapPin className="w-4 h-4 text-[#c8102e]" />
                <span>West Covina, Southern California</span>
              </div>

            </div>
          </div>

          {/* Right Column: Full Bio Narrative */}
          <div className="lg:col-span-7 space-y-8 text-left">
            
            <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-blue-50 border border-blue-200 text-[#00205b] text-xs font-bold uppercase tracking-wider">
              <span>Authority & Competitive Insight</span>
            </div>

            <h3 className="font-display text-3xl sm:text-4xl font-black text-[#00205b] uppercase leading-tight">
              Bridging Tournament Insight With{' '}
              <span className="text-[#c8102e]">Accessible Physical Instruction</span>
            </h3>

            {/* Official Bio Paragraphs */}
            <div className="space-y-5 text-slate-700 text-base leading-relaxed font-body">
              <p className="bg-slate-50 p-5 rounded-2xl border border-slate-200 font-semibold text-slate-900 leading-relaxed">
                "Great bowling is a precise science of repetition, physics, and biomechanics. As an active PBA member, USBC Level 1 Certified Coach (actively pursuing USBC Bronze), and competitive tournament bowler, I help athletes see what they cannot feel in their own physical game."
              </p>

              <p>
                My mission goes beyond basic drills: I am committed to preserving bowling as a true sport and athletic discipline. Whether working with two-handers looking to harness high-rev leverage or traditional one-handers mastering their swing plane, I bring a sharp <strong className="text-[#00205b]">"Coach’s Eye"</strong> that catches the most minute mechanical variations.
              </p>

              <p>
                Backed by elite on-lane achievements—including a <strong className="text-slate-900 font-bold">730 sanctioned high series, sanctioned 300 games, and PBA Regional competition</strong>—I bridge high-level competitive insight with accessible, actionable instruction tailored directly to your physical build and comfort.
              </p>
            </div>

            {/* Action Buttons */}
            <div className="pt-3 flex flex-wrap items-center gap-4">
              <button
                onClick={onBookClick}
                className="bg-[#c8102e] hover:bg-[#a60d24] text-white text-xs sm:text-sm font-bold uppercase tracking-wider px-7 py-4 rounded-xl flex items-center gap-2 shadow-md cursor-pointer border border-[#a60d24]"
              >
                <Calendar className="w-4 h-4" />
                <span>Book a Coaching Session</span>
              </button>

              <button
                onClick={onOpenDiagnostic}
                className="px-6 py-4 rounded-xl text-xs sm:text-sm font-bold text-[#00205b] bg-slate-100 hover:bg-slate-200 border border-slate-300 transition-colors flex items-center gap-2 cursor-pointer"
              >
                <FileCheck className="w-4 h-4 text-[#c8102e]" />
                <span>Complete Baseline Diagnostic</span>
              </button>
            </div>

          </div>

        </div>
      </div>

      {/* Section 2: Coaching Philosophy (3 Pillars) */}
      <div className="space-y-12">
        
        <div className="text-center max-w-3xl mx-auto space-y-4">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-red-50 border border-red-200 text-[#c8102e] text-xs font-bold uppercase tracking-wider">
            <Compass className="w-3.5 h-3.5" />
            <span>Core Methodology</span>
          </div>

          <h3 className="font-display text-3xl sm:text-4xl font-black uppercase text-[#00205b] tracking-tight">
            The Three Pillars of the{' '}
            <span className="text-[#c8102e]">
              Quilarquez Coaching System
            </span>
          </h3>

          <p className="text-slate-600 text-base sm:text-lg leading-relaxed font-body">
            How we take complex athletic movement and transform it into natural, repeatable power.
          </p>
        </div>

        {/* 3 Philosophy Cards with Exact Line Up & Equal Height */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 items-stretch">
          
          {/* Pillar 1 */}
          <div className="card-usbc p-8 sm:p-9 border-t-4 border-t-[#00205b] flex flex-col justify-between h-full shadow-sm">
            <div>
              <div className="w-14 h-14 rounded-2xl bg-blue-50 border border-blue-200 text-[#00205b] flex items-center justify-center mb-6 shadow-xs">
                <Video className="w-7 h-7" />
              </div>

              <div className="text-xs font-bold text-[#c8102e] uppercase tracking-widest mb-1.5">
                Pillar 01
              </div>
              <h4 className="font-display text-xl sm:text-2xl font-bold text-[#00205b] mb-3">
                Biomechanics & Video Analysis
              </h4>
              <p className="text-slate-600 text-sm leading-relaxed mb-6 font-body">
                Breaking down complex physical timing, axis rotation, tilt, and foul-line leverage into simple, actionable visual steps using 240fps frame-by-frame capture.
              </p>
            </div>

            <div className="pt-5 border-t border-slate-100 text-xs font-bold text-emerald-700 flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-600" />
              <span>Instant visual feedback on-lane</span>
            </div>
          </div>

          {/* Pillar 2 */}
          <div className="card-usbc p-8 sm:p-9 border-t-4 border-t-[#c8102e] flex flex-col justify-between h-full shadow-md">
            <div>
              <div className="w-14 h-14 rounded-2xl bg-red-50 border border-red-200 text-[#c8102e] flex items-center justify-center mb-6 shadow-xs">
                <Target className="w-7 h-7" />
              </div>

              <div className="text-xs font-bold text-[#c8102e] uppercase tracking-widest mb-1.5">
                Pillar 02
              </div>
              <h4 className="font-display text-xl sm:text-2xl font-bold text-[#00205b] mb-3">
                Adaptable to the Athlete
              </h4>
              <p className="text-slate-600 text-sm leading-relaxed mb-6 font-body">
                No one-size-fits-all approach. We tailor swing paths, foot tempo, and leverage points to your body mechanics, natural flexibility, bowling style, and personal goals.
              </p>
            </div>

            <div className="pt-5 border-t border-slate-100 text-xs font-bold text-emerald-700 flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-600" />
              <span>Tailored for 1-Hand & 2-Hand Styles</span>
            </div>
          </div>

          {/* Pillar 3 */}
          <div className="card-usbc p-8 sm:p-9 border-t-4 border-t-[#c39d5e] flex flex-col justify-between h-full shadow-sm">
            <div>
              <div className="w-14 h-14 rounded-2xl bg-[#c39d5e]/15 border border-[#c39d5e]/30 text-[#c39d5e] flex items-center justify-center mb-6 shadow-xs">
                <Compass className="w-7 h-7" />
              </div>

              <div className="text-xs font-bold text-[#b08b4b] uppercase tracking-widest mb-1.5">
                Pillar 03
              </div>
              <h4 className="font-display text-xl sm:text-2xl font-bold text-[#00205b] mb-3">
                Understanding the "Why"
              </h4>

              <p className="text-slate-600 text-sm leading-relaxed mb-6 font-body">
                Ensuring the student understands the physics and 'why' behind every adjustment to build repeatable confidence, self-diagnosis, and composure under pressure.
              </p>
            </div>

            <div className="pt-5 border-t border-slate-100 text-xs font-bold text-emerald-700 flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-600" />
              <span>Self-correction in the 10th frame</span>
            </div>
          </div>

        </div>

      </div>

    </div>
  );
};
