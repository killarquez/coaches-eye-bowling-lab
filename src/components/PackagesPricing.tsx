import { 
  Check, 
  X, 
  Calendar, 
  ChevronRight, 
  Clock, 
  Layers,
  Flame
} from 'lucide-react';
import { COACHING_PACKAGES } from '../data/coachingData';
import type { CoachingPackage } from '../types';

interface PackagesPricingProps {
  onSelectPackage: (pkg: CoachingPackage) => void;
  onOpenDiagnostic: (packageId?: string) => void;
}

export const PackagesPricing: React.FC<PackagesPricingProps> = ({ 
  onSelectPackage, 
  onOpenDiagnostic 
}) => {
  const comparisonMatrix = [
    {
      feature: "Format & Cohort Size",
      tuneUp: "1-on-1 Private",
      devPkg: "1-on-1 Comprehensive",
      camp: "Group Clinic (2–6 Bowlers)",
      strategy: "1-on-1 Match Play"
    },
    {
      feature: "Session Length & Frequency",
      tuneUp: "60 Minutes (Single)",
      devPkg: "5 x 60 Min Sessions",
      camp: "4 Wks (90–120 Min/Wk)",
      strategy: "75 Min (Live Game Play)"
    },
    {
      feature: "Tiered Per-Person Pricing",
      tuneUp: "$65 / session",
      devPkg: "$295 / 5 sessions",
      camp: "$150 – $180 / person",
      strategy: "$85 / session"
    },

    {
      feature: "240 FPS Slow-Motion Video Analysis",
      tuneUp: true,
      devPkg: true,
      camp: true,
      strategy: true
    },
    {
      feature: "Slide Foot & Leverage Audit",
      tuneUp: true,
      devPkg: true,
      camp: true,
      strategy: true
    },
    {
      feature: "3-6-9 Spare Matrix System Mastery",
      tuneUp: false,
      devPkg: true,
      camp: true,
      strategy: true
    },
    {
      feature: "Arsenal & Ball Layout Review (Pro Shop Coordinated)",
      tuneUp: false,
      devPkg: true,
      camp: true,
      strategy: true
    },
    {
      feature: "Multi-Week Progression Blueprint & Drill Library",
      tuneUp: false,
      devPkg: true,
      camp: true,
      strategy: false
    },
    {
      feature: "Direct Coach Text & Video Form Check Access",
      tuneUp: false,
      devPkg: true,
      camp: false,
      strategy: false
    },
    {
      feature: "Live Tournament Block Shadowing & Friction Moves",
      tuneUp: false,
      devPkg: false,
      camp: false,
      strategy: true
    }
  ];

  return (
    <div className="space-y-20">
      
      {/* 4 Core Pricing Cards with Exact Line Up & Equal Height & Roomy Top Clearance */}
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
                <div className="flex items-center justify-between text-xs font-bold text-[#00205b] mb-3">
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

                <h3 className="font-display text-xl font-bold text-[#00205b] mb-1.5">
                  {pkg.title}
                </h3>

                <p className="text-xs font-semibold text-slate-500 mb-4 min-h-[32px]">
                  {pkg.subtitle}
                </p>

                <div className="py-4 border-y border-slate-100 my-3 flex items-baseline gap-1.5">
                  <span className="font-display text-3xl sm:text-4xl font-black text-[#00205b]">
                    {pkg.id === 'group-camps' ? '$150–$180' : `$${pkg.price}`}
                  </span>
                  <span className="text-xs font-semibold text-slate-500">
                    {pkg.id === 'development-package' ? '/ 5 sessions' : pkg.id === 'group-camps' ? '/ person (4-Wk Clinic)' : '/ session'}
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

                <p className="text-xs text-slate-600 mb-5 leading-relaxed bg-slate-50 p-3 rounded-xl border border-slate-200 min-h-[64px]">
                  <span className="text-[#00205b] font-bold block mb-0.5">Best For:</span>
                  {pkg.idealFor}
                </p>

                <div className="space-y-2.5 mb-8 flex-grow">
                  {pkg.features.map((feat, idx) => (
                    <div key={idx} className="flex items-start gap-2.5 text-xs text-slate-700">
                      <Check className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5 font-bold" />
                      <span className="font-medium">{feat}</span>
                    </div>
                  ))}
                </div>
              </div>


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
                  <span>Select Package</span>
                  <ChevronRight className="w-3.5 h-3.5" />
                </button>

                <button
                  onClick={() => onOpenDiagnostic(pkg.id)}
                  className="w-full text-center text-xs font-bold text-slate-500 hover:text-[#00205b] py-1 cursor-pointer"
                >
                  Run Baseline Diagnostic →
                </button>
              </div>

            </div>
          );
        })}
      </div>

      {/* Feature Comparison Matrix Table with Roomy Padding */}
      <div className="card-usbc p-8 sm:p-12 border border-slate-200 shadow-md overflow-hidden">
        
        <div className="text-center max-w-2xl mx-auto mb-10 space-y-2">
          <div className="inline-flex items-center gap-2 px-3.5 py-1 rounded-full bg-blue-50 text-[#00205b] text-xs font-bold uppercase">
            <Layers className="w-4 h-4 text-[#c8102e]" />
            <span>Comprehensive Comparison</span>
          </div>
          <h3 className="font-display text-2xl sm:text-3xl font-black uppercase text-[#00205b]">
            Side-by-Side Deliverables Matrix
          </h3>
          <p className="text-xs sm:text-sm text-slate-500">
            Compare coaching inclusions, homework regimens, and video analysis deliverables across all 4 tiers
          </p>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse min-w-[700px]">
            <thead>
              <tr className="border-b-2 border-slate-200 text-xs font-bold text-slate-500 uppercase bg-slate-50">
                <th className="py-4.5 px-5 font-bold text-[#00205b]">Coaching Deliverable</th>
                <th className="py-4.5 px-5 text-center">Tune-Up ($65)</th>
                <th className="py-4.5 px-5 text-center text-[#c8102e] bg-red-50/50">Development ($295)</th>
                <th className="py-4.5 px-5 text-center">Camps ($180)</th>
                <th className="py-4.5 px-5 text-center">Strategy ($85)</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-xs">
              {comparisonMatrix.map((row, rIdx) => (
                <tr key={rIdx} className="hover:bg-slate-50 transition-colors">
                  <td className="py-4 px-5 font-bold text-slate-800">
                    {row.feature}
                  </td>

                  {/* Tune-up */}
                  <td className="py-4 px-5 text-center">
                    {typeof row.tuneUp === 'boolean' ? (
                      row.tuneUp ? (
                        <Check className="w-5 h-5 text-emerald-600 mx-auto font-bold" />
                      ) : (
                        <X className="w-5 h-5 text-slate-300 mx-auto" />
                      )
                    ) : (
                      <span className="text-slate-800 font-bold">{row.tuneUp}</span>
                    )}
                  </td>

                  {/* Development */}
                  <td className="py-4 px-5 text-center bg-red-50/30">
                    {typeof row.devPkg === 'boolean' ? (
                      row.devPkg ? (
                        <Check className="w-5 h-5 text-[#c8102e] mx-auto font-black" />
                      ) : (
                        <X className="w-5 h-5 text-slate-300 mx-auto" />
                      )
                    ) : (
                      <span className="text-[#c8102e] font-bold">{row.devPkg}</span>
                    )}
                  </td>

                  {/* Camps */}
                  <td className="py-4 px-5 text-center">
                    {typeof row.camp === 'boolean' ? (
                      row.camp ? (
                        <Check className="w-5 h-5 text-emerald-600 mx-auto font-bold" />
                      ) : (
                        <X className="w-5 h-5 text-slate-300 mx-auto" />
                      )
                    ) : (
                      <span className="text-slate-800 font-bold">{row.camp}</span>
                    )}
                  </td>

                  {/* Strategy */}
                  <td className="py-4 px-5 text-center">
                    {typeof row.strategy === 'boolean' ? (
                      row.strategy ? (
                        <Check className="w-5 h-5 text-emerald-600 mx-auto font-bold" />
                      ) : (
                        <X className="w-5 h-5 text-slate-300 mx-auto" />
                      )
                    ) : (
                      <span className="text-slate-800 font-bold">{row.strategy}</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

      </div>

    </div>
  );
};
