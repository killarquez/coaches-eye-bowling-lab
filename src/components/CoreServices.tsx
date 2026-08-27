import { 
  Clock, 
  Check, 
  ChevronRight, 
  Calendar, 
  Target, 
  Layers,
  Flame
} from 'lucide-react';
import { COACHING_PACKAGES } from '../data/coachingData';
import type { CoachingPackage } from '../types';

interface CoreServicesProps {
  onSelectPackage: (pkg: CoachingPackage) => void;
  onOpenDiagnostic: (packageId?: string) => void;
  onViewAllPackages: () => void;
}


export const CoreServices: React.FC<CoreServicesProps> = ({ 
  onSelectPackage, 
  onOpenDiagnostic,
  onViewAllPackages 
}) => {
  return (
    <section id="packages-overview" className="py-24 sm:py-28 bg-slate-50 relative overflow-hidden">
      
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10 space-y-16">
        
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto space-y-4">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-blue-50 border border-blue-200 text-[#00205b] text-xs font-bold uppercase tracking-wider">
            <Target className="w-3.5 h-3.5 text-[#c8102e]" />
            <span>Structured Coaching Programs</span>
          </div>

          <h2 className="font-display text-3xl sm:text-4xl lg:text-5xl font-black uppercase text-[#00205b] tracking-tight">
            Tailored Instruction for{' '}
            <span className="text-[#c8102e]">
              Every Bowler
            </span>
          </h2>

          <p className="text-slate-600 text-base sm:text-lg leading-relaxed font-body pt-2">
            From single-pin spare calibration to full biomechanical restructuring, select the program designed for your physical game at Bowlero West Covina.
          </p>
        </div>

        {/* 4-Card Grid with Crisp White USBC Cards & Roomy Top Clearance */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8 items-stretch pt-4">
          {COACHING_PACKAGES.map((pkg) => {
            const isPopular = pkg.popular;
            
            return (
              <div
                key={pkg.id}
                className={`card-usbc p-8 flex flex-col justify-between h-full relative ${
                  isPopular 
                    ? 'border-2 border-[#c8102e] shadow-xl ring-4 ring-red-50' 
                    : 'border border-slate-200 shadow-sm'
                }`}
              >
                {/* Top Pulsing Deal Badge */}
                {pkg.badge && (
                  <div className="absolute -top-4.5 left-1/2 -translate-x-1/2 z-20 whitespace-nowrap">
                    <span className="inline-flex items-center gap-1.5 px-4 py-1.5 rounded-full text-xs font-black tracking-wider uppercase bg-[#c8102e] text-white shadow-xl border-2 border-white ring-2 ring-[#c8102e]/30 badge-deal-pulse">
                      <Flame className="w-3.5 h-3.5 text-amber-300 fill-amber-300 shrink-0" />
                      <span>{pkg.badge}</span>
                    </span>
                  </div>
                )}


                <div className="flex flex-col flex-grow">
                  {/* Duration & Format Tag */}
                  <div className="flex items-center justify-between text-xs font-bold text-slate-500 mb-3">
                    <span className="flex items-center gap-1.5 text-[#00205b]">
                      <Clock className="w-3.5 h-3.5 text-[#c8102e]" />
                      {pkg.duration}
                    </span>
                    <span className={`text-[11px] font-bold px-2 py-0.5 rounded ${
                      pkg.id === 'group-camps' 
                        ? 'bg-purple-100 text-purple-900 border border-purple-200' 
                        : 'text-slate-500 bg-slate-100'
                    }`}>
                      {pkg.formatType || '1-on-1 Private'}
                    </span>
                  </div>

                  {/* Title & Subtitle */}
                  <h3 className="font-display text-xl font-bold text-[#00205b] mb-1.5">
                    {pkg.title}
                  </h3>
                  <p className="text-xs font-semibold text-slate-500 mb-4 min-h-[32px]">
                    {pkg.subtitle}
                  </p>

                  {/* Price Block */}
                  <div className="mb-4 pb-4 border-b border-slate-100 flex items-baseline gap-1.5">
                    <span className="font-display text-3xl sm:text-4xl font-black text-[#00205b]">
                      {pkg.id === 'group-camps' ? '$150–$180' : `$${pkg.price}`}
                    </span>
                    <span className="text-xs font-semibold text-slate-500">
                      {pkg.id === 'group-camps' ? '/ person (4-Wk Clinic)' : pkg.id === 'development-package' ? '/ 5 sessions' : '/ session'}
                    </span>
                  </div>

                  {/* Group Clinic Tier Matrix Card (If Available) */}
                  {pkg.groupTiers && (
                    <div className="mb-4 bg-purple-50/80 border border-purple-200 rounded-xl p-3 text-[11px] space-y-1.5 shadow-xs">
                      <div className="font-bold text-[#00205b] text-[10.5px] uppercase flex items-center justify-between">
                        <span>👥 Group Size Rates:</span>
                        <span className="text-[#c8102e] font-extrabold">Bigger Group = Cheaper</span>
                      </div>
                      <div className="grid grid-cols-2 gap-1 text-[10px]">
                        <div className="bg-white p-1.5 rounded border border-purple-100">
                          <span className="font-bold text-slate-900">1–2 Bowlers:</span> $180/pp (90m)
                        </div>
                        <div className="bg-white p-1.5 rounded border border-purple-100">
                          <span className="font-bold text-slate-900">3–4 Bowlers:</span> $170/pp (105m)
                        </div>
                        <div className="bg-white p-1.5 rounded border border-purple-100">
                          <span className="font-bold text-slate-900">5 Bowlers:</span> $160/pp (120m)
                        </div>
                        <div className="bg-white p-1.5 rounded border border-purple-100 font-bold text-[#c8102e]">
                          <span>6 Bowlers:</span> $150/pp (120m)
                        </div>
                      </div>
                    </div>
                  )}

                  {/* Summary / Ideal For */}
                  <p className="text-xs text-slate-600 mb-5 leading-relaxed bg-slate-50 p-3 rounded-xl border border-slate-200 min-h-[64px]">
                    <strong className="text-[#00205b] block mb-0.5 font-bold">Ideal For:</strong> {pkg.idealFor}
                  </p>


                  {/* Feature Checklist */}
                  <div className="space-y-3 mb-8 flex-grow">
                    <div className="text-[11px] font-bold uppercase tracking-wider text-slate-500">
                      What's Included:
                    </div>
                    {pkg.features.slice(0, 3).map((feat, fIdx) => (
                      <div key={fIdx} className="flex items-start gap-2.5 text-xs text-slate-700">
                        <Check className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5 font-bold" />
                        <span className="font-medium">{feat}</span>
                      </div>
                    ))}
                    {pkg.features.length > 3 && (
                      <div className="text-[11px] font-bold text-slate-400 pl-6.5">
                        + {pkg.features.length - 3} more deliverables
                      </div>
                    )}
                  </div>
                </div>

                {/* Card CTA Buttons - Perfectly Aligned at Bottom */}
                <div className="space-y-2.5 pt-5 border-t border-slate-100">
                  <button
                    onClick={() => onSelectPackage(pkg)}
                    className={`w-full py-4 px-4 rounded-xl text-xs sm:text-sm font-bold uppercase tracking-wider flex items-center justify-center gap-2 cursor-pointer transition-all ${
                      isPopular 
                        ? 'bg-[#c8102e] hover:bg-[#a60d24] text-white shadow-md border border-[#a60d24]' 
                        : 'bg-[#00205b] hover:bg-[#0b2e7a] text-white shadow-sm'
                    }`}
                  >
                    <Calendar className="w-4 h-4" />
                    <span>Book Package</span>
                    <ChevronRight className="w-3.5 h-3.5" />
                  </button>

                  <button
                    onClick={() => onOpenDiagnostic(pkg.id)}
                    className="w-full text-center text-xs font-bold text-slate-500 hover:text-[#00205b] transition-colors py-1 cursor-pointer"
                  >
                    Take Baseline Diagnostic →
                  </button>
                </div>

              </div>
            );
          })}
        </div>

        {/* View Full Comparison Button */}
        <div className="pt-4 text-center">
          <button
            onClick={onViewAllPackages}
            className="inline-flex items-center gap-2.5 px-8 py-4 rounded-xl bg-white border border-slate-300 hover:border-[#00205b] text-sm font-bold text-[#00205b] hover:bg-slate-50 shadow-sm transition-all cursor-pointer"
          >
            <Layers className="w-4 h-4 text-[#c8102e]" />
            <span>View Full Feature Comparison Matrix</span>
            <ChevronRight className="w-4 h-4 text-slate-400" />
          </button>
        </div>

      </div>
    </section>
  );
};
