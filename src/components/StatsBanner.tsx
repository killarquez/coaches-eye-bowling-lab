import React from 'react';
import { Trophy, Flame, ShieldCheck, Award } from 'lucide-react';

export const StatsBanner: React.FC = () => {
  const stats = [
    {
      value: "730",
      label: "Official Sanctioned Series",
      sub: "Career High Series Mark",
      icon: Trophy,
      iconColor: "text-[#c39d5e]",
      bgColor: "bg-[#c39d5e]/15 border-[#c39d5e]/30"
    },

    {
      value: "2x",
      label: "Sanctioned 300 Games",
      sub: "Dozens in Tournament Practice",
      icon: Flame,
      iconColor: "text-[#c8102e]",
      bgColor: "bg-red-50 border-red-200"
    },
    {
      value: "PBA",
      label: "Active PBA Member",
      sub: "Western Regional Competitor",
      icon: ShieldCheck,
      iconColor: "text-[#00205b]",
      bgColor: "bg-blue-50 border-blue-200"
    },
    {
      value: "Level 1",
      label: "USBC Certified Coach",
      sub: "SafeSport & RVP Cleared",
      icon: Award,
      iconColor: "text-emerald-600",
      bgColor: "bg-emerald-50 border-emerald-200"
    }
  ];

  return (
    <section className="relative z-20 -mt-10 sm:-mt-12 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div className="bg-white border border-slate-200 rounded-3xl shadow-xl p-8 sm:p-10 lg:p-12">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8 lg:gap-10 divide-y sm:divide-y-0 sm:divide-x divide-slate-200">
          {stats.map((item, idx) => {
            const Icon = item.icon;
            return (
              <div 
                key={idx} 
                className={`text-center space-y-3 flex flex-col items-center justify-between ${
                  idx > 0 ? 'pt-8 sm:pt-0 sm:pl-8 lg:pl-10' : ''
                }`}
              >
                <div className={`inline-flex items-center justify-center w-14 h-14 rounded-2xl ${item.bgColor} border mb-2 shadow-xs`}>
                  <Icon className={`w-7 h-7 ${item.iconColor}`} />
                </div>
                
                <div className="font-display text-4xl sm:text-5xl font-black text-[#00205b] tracking-tight leading-none">
                  {item.value}
                </div>

                <div className="space-y-1">
                  <div className="text-sm font-extrabold text-slate-800 uppercase tracking-tight">
                    {item.label}
                  </div>

                  <div className="text-xs font-semibold text-slate-500">
                    {item.sub}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
};
