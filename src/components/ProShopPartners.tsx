import React from 'react';
import { 
  MapPin, 
  Phone, 
  CheckCircle2, 
  Sparkles, 
  Compass
} from 'lucide-react';
import { PRO_SHOP_PARTNERS } from '../data/coachingData';

export const ProShopPartners: React.FC = () => {
  return (
    <div className="space-y-20">
      
      {/* Top Banner Hook */}
      <div className="bg-slate-50 border-2 border-blue-100 rounded-3xl p-8 sm:p-10 text-center max-w-4xl mx-auto shadow-xs">
        <p className="text-slate-800 text-base sm:text-lg leading-relaxed font-body font-semibold">
          "Need ball drilling, surface adjustments, or layout optimization? We proudly collaborate with <strong className="text-[#00205b]">Pro Approach</strong> and <strong className="text-[#c8102e]">Axis Point Bowling</strong> in West Covina to match your physical game with the right arsenal."
        </p>
      </div>

      {/* 2 Pro Shop Partner Feature Cards with Exact Line Up & Equal Height */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8 items-stretch">
        {PRO_SHOP_PARTNERS.map((partner, idx) => {
          const isFirst = idx === 0;

          return (
            <div
              key={idx}
              className={`card-usbc p-8 sm:p-10 flex flex-col justify-between h-full border ${
                isFirst
                  ? 'border-t-4 border-t-[#00205b] shadow-md'
                  : 'border-t-4 border-t-[#c8102e] shadow-md'
              }`}
            >
              <div>
                {/* Location & Badge */}
                <div className="flex items-center justify-between mb-5">
                  <span className="inline-flex items-center gap-1.5 text-xs font-bold text-[#00205b] bg-blue-50 px-3.5 py-1 rounded-full border border-blue-200">
                    <MapPin className="w-3.5 h-3.5 text-[#c8102e]" />
                    {partner.location} ({partner.distance})
                  </span>
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">
                    IBPSIA CERTIFIED
                  </span>
                </div>

                <h3 className="font-display text-2xl sm:text-3xl font-black text-[#00205b] mb-2">
                  {partner.name}
                </h3>

                <div className="text-xs font-bold text-[#c8102e] mb-5">
                  {partner.specialty}
                </div>

                <p className="text-slate-600 text-sm leading-relaxed mb-6 font-body">
                  {partner.description}
                </p>

                {/* Services Checklist */}
                <div className="space-y-2.5 mb-8 bg-slate-50 p-5 rounded-2xl border border-slate-200">
                  <div className="text-[11px] font-bold uppercase tracking-wider text-slate-700 mb-2">
                    Partner Services for Coaching Students:
                  </div>
                  {partner.services.map((svc, sIdx) => (
                    <div key={sIdx} className="flex items-center gap-2.5 text-xs font-semibold text-slate-700">
                      <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 font-bold" />
                      <span>{svc}</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Bottom Contact & Sync Badge */}
              <div className="pt-5 border-t border-slate-100 flex items-center justify-between">
                <div className="flex items-center gap-2 text-xs font-bold text-slate-700">
                  <Phone className="w-3.5 h-3.5 text-[#c8102e]" />
                  <span>{partner.contact}</span>
                </div>

                <div className="inline-flex items-center gap-1.5 text-xs font-bold text-[#00205b] bg-blue-50 px-3.5 py-2 rounded-xl border border-blue-200">
                  <Sparkles className="w-3.5 h-3.5 text-amber-500" />
                  <span>Coach Spec Handoff</span>
                </div>
              </div>

            </div>
          );
        })}
      </div>

      {/* The Mechanics & Arsenal Synergy Guide */}
      <div className="card-usbc p-8 sm:p-12 border border-slate-200 bg-white shadow-sm">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
          <div className="lg:col-span-8 space-y-3">
            <div className="inline-flex items-center gap-2 text-xs font-bold text-[#c8102e] uppercase">
              <Compass className="w-4 h-4" />
              <span>Why Arsenal Matching Matters</span>
            </div>
            <h3 className="font-display text-2xl sm:text-3xl font-black text-[#00205b] uppercase">
              How Coach Alfredo Works Hand-In-Hand With Your Driller
            </h3>
            <p className="text-slate-600 text-sm leading-relaxed font-body pt-1">
              A great swing can never overcome a poorly fitted thumb pitch, improper span, or incorrect dual-angle layout that forces early hook in the front oil. During our video sessions, we calculate your <strong className="text-slate-900 font-bold">Positive Axis Point (PAP), Axis Tilt, and Rev-to-Speed Ratio</strong> to hand your driller exact layout specifications.
            </p>
          </div>

          <div className="lg:col-span-4 text-center lg:text-right">
            <button
              onClick={() => {
                const elem = document.getElementById('booking-view');
                if (elem) elem.scrollIntoView({ behavior: 'smooth' });
              }}
              className="inline-flex items-center gap-2 bg-[#00205b] hover:bg-[#0b2e7a] text-white font-bold text-xs uppercase tracking-wider px-7 py-4.5 rounded-xl shadow-md cursor-pointer transition-all"
            >
              <span>Book Biomechanical Ball Audit</span>
            </button>
          </div>
        </div>
      </div>

    </div>
  );
};
