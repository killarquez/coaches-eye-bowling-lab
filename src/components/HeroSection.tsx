import React from 'react';
import { 
  Calendar, 
  ChevronRight, 
  MapPin, 
  Trophy, 
  Award, 
  CheckCircle2, 
  Video, 
  Target, 
  ArrowRight 
} from 'lucide-react';

interface HeroSectionProps {
  onBookClick: () => void;
  onViewPackages: () => void;
  onOpenDiagnostic: () => void;
  onLearnMore: () => void;
}

export const HeroSection: React.FC<HeroSectionProps> = ({
  onBookClick,
  onViewPackages,
  onOpenDiagnostic,
  onLearnMore
}) => {
  return (
    <section className="relative usbc-navy-gradient text-white py-20 sm:py-24 lg:py-28 overflow-hidden border-b border-blue-900">
      
      {/* Background Graphic Accent */}
      <div className="absolute right-0 top-1/2 -translate-y-1/2 w-[700px] h-[700px] bg-blue-400/5 rounded-full blur-3xl pointer-events-none" />
      
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-16 items-center">
          
          {/* Left Column: Authoritative Copy & Clear CTAs - Clean Left Aligned */}
          <div className="lg:col-span-7 space-y-8 text-left">
            
            {/* Badges Strip */}
            <div className="flex flex-wrap items-center gap-3">
              <span className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full text-xs font-bold bg-white/10 text-white border border-white/20 shadow-xs">
                <MapPin className="w-3.5 h-3.5 text-[#c8102e]" />
                <span>Bowlero West Covina, CA</span>
              </span>

              <span className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full text-xs font-bold bg-[#c39d5e]/20 text-[#d5b57a] border border-[#c39d5e]/30 shadow-xs">
                <Trophy className="w-3.5 h-3.5 text-[#c39d5e]" />
                <span>730 Series • 2x 300</span>
              </span>

              <span className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full text-xs font-bold bg-red-600/30 text-red-200 border border-red-500/40 shadow-xs">
                <Award className="w-3.5 h-3.5 text-red-400" />
                <span>USBC Level 1 & PBA Member</span>
              </span>
            </div>

            {/* Main Headline */}
            <div className="space-y-3">
              <h1 className="font-display text-4xl sm:text-5xl lg:text-6xl font-black tracking-tight text-white uppercase leading-[1.12]">
                Master the Lanes.{' '}
                <span className="text-[#c39d5e] block mt-2">
                  Elevate Your Physical Game.
                </span>
              </h1>

              {/* Exact Subheadline from prompt */}
              <p className="text-base sm:text-lg text-slate-200 leading-relaxed font-body max-w-2xl pt-2">
                USBC Level 1 & PBA Member instruction based out of <strong className="text-white">Bowlero West Covina</strong>. Whether you are building solid fundamentals or refining elite tournament mechanics, unlock a repeatable, powerful game through biomechanical analysis.
              </p>
            </div>

            {/* Quick Benefits for League & Tournament Bowlers - Generous gap & padding */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-1">
              <div className="flex items-center gap-3 text-sm text-slate-100 bg-white/5 border border-white/10 p-4 rounded-xl shadow-xs">
                <div className="w-9 h-9 rounded-lg bg-[#c39d5e]/10 flex items-center justify-center shrink-0">
                  <Video className="w-5 h-5 text-[#c39d5e]" />
                </div>
                <span className="font-semibold leading-snug">240 FPS Slow-Motion Video Review</span>
              </div>

              <div className="flex items-center gap-3 text-sm text-slate-100 bg-white/5 border border-white/10 p-4 rounded-xl shadow-xs">
                <div className="w-9 h-9 rounded-lg bg-red-500/10 flex items-center justify-center shrink-0">
                  <Target className="w-5 h-5 text-[#c8102e]" />
                </div>
                <span className="font-semibold leading-snug">1-Handed & 2-Handed Power Leverage</span>
              </div>
            </div>

            {/* Action Buttons with Roomy Spacing */}
            <div className="pt-3 flex flex-col sm:flex-row items-stretch sm:items-center gap-4">
              <button
                onClick={onBookClick}
                className="bg-[#c8102e] hover:bg-[#a60d24] text-white text-base font-bold uppercase tracking-wider px-8 py-4.5 rounded-xl flex items-center justify-center gap-2.5 shadow-xl hover:shadow-2xl transition-all cursor-pointer border border-[#a60d24]"
              >
                <Calendar className="w-5 h-5" />
                <span>Book a Session ($65+)</span>
                <ChevronRight className="w-4 h-4" />
              </button>

              <button
                onClick={onViewPackages}
                className="bg-white/10 hover:bg-white/20 text-white font-bold text-sm px-7 py-4.5 rounded-xl border border-white/30 transition-all flex items-center justify-center gap-2 cursor-pointer"
              >
                <span>View Coaching Packages</span>
                <ArrowRight className="w-4 h-4 text-[#c39d5e]" />
              </button>
            </div>


            {/* Trust Indicator - Perfectly aligned */}
            <div className="pt-2 flex flex-wrap items-center gap-5 text-xs font-semibold text-slate-300">
              <span className="flex items-center gap-1.5 text-emerald-400">
                <CheckCircle2 className="w-4 h-4 shrink-0" />
                <span>SafeSport & RVP Cleared</span>
              </span>
              <span>•</span>
              <span>Individual & Cohort Instruction</span>
              <span>•</span>
              <span>Lane Fees Included</span>
            </div>

          </div>

          {/* Right Column: Clean USBC Coach & Diagnostic Showcase Card - Perfectly Centered & Aligned */}
          <div className="lg:col-span-5">
            <div className="bg-white text-slate-900 rounded-3xl p-7 sm:p-9 shadow-2xl border border-slate-200 relative space-y-6">
              
              {/* Card Header with USBC Red Header Band */}
              <div className="flex items-center justify-between border-b border-slate-100 pb-5">
                <div className="flex items-center gap-4">
                  <div className="w-14 h-14 rounded-2xl bg-[#0b1e3b] text-white flex items-center justify-center p-0.5 shadow-md border-2 border-[#c8102e] overflow-hidden shrink-0">
                    <img 
                      src="/logo-emblem.png" 
                      alt="Coach's Eye Bowling Lab Emblem" 
                      className="w-full h-full object-cover"
                    />
                  </div>
                  <div>
                    <div className="font-display font-extrabold text-lg sm:text-xl text-[#00205b] uppercase tracking-tight">
                      Alfredo Quilarquez
                    </div>
                    <div className="text-xs font-bold text-[#c8102e] uppercase tracking-wide mt-0.5">
                      PBA Member & USBC Coach
                    </div>
                  </div>
                </div>
              </div>


              {/* High-Contrast Highlights Box with Roomy Margins */}
              <div className="space-y-3.5 text-sm">
                <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 flex items-center justify-between">
                  <span className="font-semibold text-slate-700">Official High Series</span>
                  <span className="font-black text-[#00205b] font-display text-base">730 Sanctioned</span>
                </div>

                <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 flex items-center justify-between">
                  <span className="font-semibold text-slate-700">Sanctioned 300 Games</span>
                  <span className="font-black text-[#c8102e] font-display text-base">2 Official (Dozens in Practice)</span>
                </div>

                <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 flex items-center justify-between">
                  <span className="font-semibold text-slate-700">Training Facility</span>
                  <span className="font-bold text-slate-900">Bowlero West Covina</span>
                </div>
              </div>

              {/* Action Links inside card with generous padding */}
              <div className="space-y-3 pt-2">
                <button
                  onClick={onOpenDiagnostic}
                  className="w-full py-4 px-4 rounded-xl bg-blue-50 hover:bg-blue-100 border border-blue-200 text-[#00205b] font-bold text-xs uppercase tracking-wider flex items-center justify-center gap-2 cursor-pointer transition-colors shadow-xs"
                >
                  <span>Free 2-Minute Diagnostic Assessment</span>
                  <ChevronRight className="w-4 h-4 text-[#c8102e]" />
                </button>

                <button
                  onClick={onLearnMore}
                  className="w-full py-2 text-center text-xs font-bold text-slate-500 hover:text-[#00205b] transition-colors cursor-pointer"
                >
                  Read Coach Alfredo's Full Bio & Philosophy →
                </button>
              </div>

            </div>
          </div>

        </div>
      </div>
    </section>
  );
};
