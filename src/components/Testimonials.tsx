import React from 'react';
import { Quote, Star, Trophy, TrendingUp, CheckCircle2 } from 'lucide-react';
import { TESTIMONIALS } from '../data/coachingData';

export const Testimonials: React.FC = () => {
  return (
    <section className="py-24 sm:py-28 bg-white border-b border-slate-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-16">
        
        {/* Header */}
        <div className="text-center max-w-3xl mx-auto space-y-4">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-[#c39d5e]/15 border border-[#c39d5e]/30 text-[#8c6724] text-xs font-bold uppercase tracking-wider">
            <Trophy className="w-4 h-4 text-[#c39d5e]" />
            <span>Proven Student Outcomes</span>
          </div>

          <h2 className="font-display text-3xl sm:text-4xl lg:text-5xl font-black uppercase text-[#00205b] tracking-tight">
            Real Results from the{' '}
            <span className="text-[#c8102e]">
              West Covina Lanes
            </span>
          </h2>

          <p className="text-slate-600 text-base sm:text-lg leading-relaxed font-body pt-2">
            Read how biomechanical video analysis and structured drill regimens transformed pin averages and tournament execution.
          </p>
        </div>

        {/* Testimonials Grid (Crisp White Cards with Exact Equal Heights) */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 items-stretch">
          {TESTIMONIALS.map((item, idx) => (
            <div
              key={idx}
              className="card-usbc p-8 sm:p-10 flex flex-col justify-between h-full border border-slate-200 shadow-sm"
            >
              <div>
                {/* Stars and Stat Badge */}
                <div className="flex items-center justify-between mb-6">
                  <div className="flex items-center gap-1 text-[#c39d5e]">
                    {[...Array(5)].map((_, sIdx) => (
                      <Star key={sIdx} className="w-5 h-5 fill-[#c39d5e] text-[#c39d5e]" />
                    ))}
                  </div>


                  <div className="inline-flex items-center gap-2 px-3.5 py-1 rounded-full bg-emerald-50 border border-emerald-200 text-xs font-bold text-emerald-800">
                    <TrendingUp className="w-4 h-4 text-emerald-600" />
                    <span>{item.statGain}</span>
                  </div>
                </div>

                {/* Quote */}
                <div className="relative mb-8">
                  <Quote className="w-10 h-10 text-blue-100 absolute -top-4 -left-4 -z-10" />
                  <p className="text-slate-800 text-base sm:text-lg leading-relaxed font-body italic">
                    "{item.quote}"
                  </p>
                </div>
              </div>

              {/* Author Details - Aligned Baseline */}
              <div className="pt-6 border-t border-slate-100 flex items-center justify-between">
                <div>
                  <div className="font-display font-bold text-[#00205b] text-base">
                    {item.author}
                  </div>
                  <div className="text-xs font-bold text-[#c8102e] mt-0.5">
                    {item.role}
                  </div>
                  <div className="text-xs text-slate-500 font-medium mt-0.5">
                    {item.league}
                  </div>
                </div>

                <div className="w-12 h-12 rounded-full bg-[#00205b] text-white flex items-center justify-center font-display font-extrabold text-base shadow-sm shrink-0">
                  {item.author.charAt(0)}
                </div>
              </div>

            </div>
          ))}
        </div>

        {/* Verification banner */}
        <div className="bg-slate-50 border border-slate-200 rounded-2xl p-5 text-center max-w-2xl mx-auto flex items-center justify-center gap-2.5 text-xs font-bold text-slate-700">
          <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
          <span>All reviews from verified USBC sanctioned league & tournament athletes at Bowlero West Covina</span>
        </div>

      </div>
    </section>
  );
};
