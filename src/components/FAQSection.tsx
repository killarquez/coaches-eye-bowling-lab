import React, { useState } from 'react';
import { ChevronDown, ChevronUp, MapPin } from 'lucide-react';
import { FAQS, COACH_INFO } from '../data/coachingData';

export const FAQSection: React.FC = () => {
  const [openIndex, setOpenIndex] = useState<number | null>(0);

  const toggleFaq = (index: number) => {
    setOpenIndex(openIndex === index ? null : index);
  };

  return (
    <div className="space-y-20">
      
      {/* FAQ Accordion List */}
      <div className="space-y-4 max-w-4xl mx-auto">
        {FAQS.map((faq, idx) => {
          const isOpen = openIndex === idx;

          return (
            <div
              key={idx}
              className="card-usbc border border-slate-200 overflow-hidden transition-all shadow-sm"
            >
              <button
                onClick={() => toggleFaq(idx)}
                className="w-full p-6 sm:p-7 text-left flex items-center justify-between gap-4 cursor-pointer focus:outline-none bg-white hover:bg-slate-50 transition-colors"
              >
                <span className="font-display font-bold text-base sm:text-lg text-[#00205b]">
                  {faq.q}
                </span>
                <div className="w-9 h-9 rounded-full bg-slate-100 text-slate-700 flex items-center justify-center shrink-0">
                  {isOpen ? <ChevronUp className="w-4 h-4 text-[#c8102e]" /> : <ChevronDown className="w-4 h-4" />}
                </div>
              </button>

              {isOpen && (
                <div className="px-6 sm:px-7 pb-7 pt-1 border-t border-slate-100 text-slate-700 text-sm sm:text-base leading-relaxed font-body bg-slate-50/50 animate-in fade-in duration-150">
                  {faq.a}
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Facility & Location Details Card */}
      <div className="card-usbc p-8 sm:p-12 max-w-4xl mx-auto border border-slate-200 bg-white shadow-sm">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-10 items-center">
          <div className="space-y-4">
            <div className="inline-flex items-center gap-1.5 px-3.5 py-1 rounded-full bg-red-50 text-[#c8102e] text-xs font-bold uppercase">
              <MapPin className="w-3.5 h-3.5" />
              <span>Official Coaching Location</span>
            </div>
            <h3 className="font-display text-2xl sm:text-3xl font-black text-[#00205b] uppercase">
              {COACH_INFO.locationName}
            </h3>
            <p className="text-slate-600 text-sm">
              {COACH_INFO.address}
            </p>
            <div className="text-xs text-slate-500 space-y-1.5 pt-2 font-medium">
              <p>• 40 Modern Synthetic Lanes with Specto Tracking</p>
              <p>• Full-service pro shop on site for quick ball adjustments</p>
              <p>• Convenient parking directly in front of the main entrance</p>
            </div>
          </div>

          <div className="bg-slate-50 p-7 sm:p-8 rounded-3xl border border-slate-200 space-y-4 text-center">
            <div className="text-xs font-bold text-[#00205b] uppercase tracking-wider">Direct Coach Inquiries</div>
            <div className="text-lg font-bold text-slate-900">{COACH_INFO.phone}</div>
            <div className="text-xs text-slate-600">{COACH_INFO.email}</div>
            <div className="pt-2">
              <a
                href={`mailto:${COACH_INFO.email}`}
                className="inline-block bg-[#c8102e] hover:bg-[#a60d24] text-white text-xs font-bold uppercase px-6 py-3.5 rounded-xl shadow-md transition-all border border-[#a60d24]"
              >
                Send Question to Coach Alfredo
              </a>
            </div>
          </div>
        </div>
      </div>

    </div>
  );
};
