import React from 'react';
import { 
  Eye, 
  Target, 
  Zap, 
  CheckCircle2, 
  ChevronRight, 
  Flame 
} from 'lucide-react';

interface MissionHookProps {
  onLearnMore: () => void;
  onBookClick: () => void;
}

export const MissionHook: React.FC<MissionHookProps> = ({ onLearnMore, onBookClick }) => {
  return (
    <section className="py-24 sm:py-28 bg-white border-y border-slate-200 relative overflow-hidden">
      
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10 space-y-16">
        
        {/* Section Heading with generous spacing */}
        <div className="text-center max-w-3xl mx-auto space-y-4">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-red-50 border border-red-200 text-[#c8102e] text-xs font-bold uppercase tracking-wider">
            <Flame className="w-4 h-4 text-[#c8102e]" />
            <span>The Competitive Difference</span>
          </div>

          <h2 className="font-display text-3xl sm:text-4xl lg:text-5xl font-black uppercase text-[#00205b] tracking-tight leading-tight">
            "Bowling is a True Sport —{' '}
            <span className="text-[#c8102e]">
              Not Just a Weekend Party."
            </span>
          </h2>

          <p className="text-slate-600 text-base sm:text-lg leading-relaxed font-body pt-2">
            Too many bowlers spend years practicing bad habits, throwing the same balls into the same friction without understanding why pin carry disappears. Coach Alfredo brings <strong className="text-slate-900 font-semibold">technical precision, structured biomechanics, and genuine sports performance</strong> back to the lanes.
          </p>
        </div>

        {/* 3 Core Cards with Clean White / Navy Design & Exact Line Up */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 items-stretch">
          
          {/* Card 1: The Biomechanical Eye */}
          <div className="card-usbc p-8 sm:p-9 flex flex-col justify-between border-t-4 border-t-[#00205b] h-full shadow-sm">
            <div>
              <div className="w-14 h-14 rounded-2xl bg-blue-50 border border-blue-200 text-[#00205b] flex items-center justify-center mb-6 shadow-xs">
                <Eye className="w-7 h-7" />
              </div>

              <div className="text-xs font-bold text-[#c8102e] uppercase tracking-widest mb-1.5">
                Micro-Mechanical Precision
              </div>
              <h3 className="font-display text-xl sm:text-2xl font-bold text-[#00205b] mb-3">
                The "Coach's Eye" Advantage
              </h3>
              <p className="text-slate-600 text-sm leading-relaxed mb-6 font-body">
                Catching the subtle 2-degree shoulder dip, 0.1-second early foot plant, or flared elbow that leaks 30% of your ball energy before it hits the pins.
              </p>
            </div>

            <ul className="space-y-2.5 text-xs font-bold text-slate-700 border-t border-slate-100 pt-5">
              <li className="flex items-center gap-2.5">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>240 FPS multi-angle video capture</span>
              </li>
              <li className="flex items-center gap-2.5">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>Slide foot balance & leverage analysis</span>
              </li>
            </ul>
          </div>

          {/* Card 2: 1-Handed & 2-Handed Mastery */}
          <div className="card-usbc p-8 sm:p-9 flex flex-col justify-between border-t-4 border-t-[#c8102e] h-full shadow-md">
            <div>
              <div className="w-14 h-14 rounded-2xl bg-red-50 border border-red-200 text-[#c8102e] flex items-center justify-center mb-6 shadow-xs">
                <Zap className="w-7 h-7" />
              </div>

              <div className="text-xs font-bold text-[#c8102e] uppercase tracking-widest mb-1.5">
                Modern Physical Evolution
              </div>
              <h3 className="font-display text-xl sm:text-2xl font-bold text-[#00205b] mb-3">
                Modern 2-Handed & Traditional 1-Handed
              </h3>
              <p className="text-slate-600 text-sm leading-relaxed mb-6 font-body">
                We specialize in the mechanics of both disciplines. Whether tuning a classic free-fall swing or unlocking two-handed torso coil and hip snap.
              </p>
            </div>

            <ul className="space-y-2.5 text-xs font-bold text-slate-700 border-t border-slate-100 pt-5">
              <li className="flex items-center gap-2.5">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>Torso spine angle preservation</span>
              </li>
              <li className="flex items-center gap-2.5">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>Rev-rate to ball-speed synchronization</span>
              </li>
            </ul>
          </div>

          {/* Card 3: Repeatable tournament confidence */}
          <div className="card-usbc p-8 sm:p-9 flex flex-col justify-between border-t-4 border-t-[#c39d5e] h-full shadow-sm">
            <div>
              <div className="w-14 h-14 rounded-2xl bg-[#c39d5e]/15 border border-[#c39d5e]/30 text-[#c39d5e] flex items-center justify-center mb-6 shadow-xs">
                <Target className="w-7 h-7" />
              </div>

              <div className="text-xs font-bold text-[#b08b4b] uppercase tracking-widest mb-1.5">
                Physics & Pressure
              </div>
              <h3 className="font-display text-xl sm:text-2xl font-bold text-[#00205b] mb-3">
                Understanding the "Why"
              </h3>

              <p className="text-slate-600 text-sm leading-relaxed mb-6 font-body">
                No guessing in the 10th frame. When you understand how friction, axis tilt, and entry angles interact, you make swift, confident in-game adjustments.
              </p>
            </div>

            <ul className="space-y-2.5 text-xs font-bold text-slate-700 border-t border-slate-100 pt-5">
              <li className="flex items-center gap-2.5">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>3-6-9 and 2-4-6 systematic spare math</span>
              </li>
              <li className="flex items-center gap-2.5">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>Oil pattern breakdown & surface moves</span>
              </li>
            </ul>
          </div>

        </div>

        {/* Action strip with roomy padding */}
        <div className="bg-slate-50 border border-slate-200 rounded-3xl p-8 sm:p-10 flex flex-col sm:flex-row items-center justify-between gap-6">
          <div className="text-left space-y-1">
            <h4 className="text-lg sm:text-xl font-bold text-[#00205b]">
              Ready to raise your league average and tournament execution?
            </h4>
            <p className="text-xs sm:text-sm text-slate-500 font-medium">
              Private and group sessions conducted at Bowlero West Covina by appointment.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-4 shrink-0">
            <button
              onClick={onLearnMore}
              className="text-xs sm:text-sm font-bold text-slate-700 hover:text-[#00205b] px-5 py-3.5 rounded-xl border border-slate-300 hover:bg-slate-100 transition-colors cursor-pointer"
            >
              Meet Coach Alfredo
            </button>
            <button
              onClick={onBookClick}
              className="bg-[#c8102e] hover:bg-[#a60d24] text-white text-xs sm:text-sm font-bold uppercase tracking-wider px-7 py-3.5 rounded-xl flex items-center gap-2 cursor-pointer shadow-md transition-all border border-[#a60d24]"
            >
              <span>Book Quick Tune-Up ($65)</span>
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>

      </div>
    </section>
  );
};
