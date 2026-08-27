import React, { useState } from 'react';
import { 
  MapPin, 
  Mail, 
  ChevronRight, 
  CheckCircle2, 
  Send,
  Phone
} from 'lucide-react';
import { COACH_INFO } from '../data/coachingData';

interface FooterProps {
  onNavClick: (id: string) => void;
  onOpenDiagnostic: () => void;
}

export const Footer: React.FC<FooterProps> = ({ onNavClick, onOpenDiagnostic }) => {
  const [emailSubscribed, setEmailSubscribed] = useState(false);
  const [newsletterEmail, setNewsletterEmail] = useState('');

  const handleNewsletter = (e: React.FormEvent) => {
    e.preventDefault();
    if (newsletterEmail) {
      setEmailSubscribed(true);
      setNewsletterEmail('');
    }
  };

  return (
    <footer className="bg-[#00205b] text-white pt-16 pb-12 border-t-4 border-[#c8102e]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Top Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-12 gap-10 pb-12 border-b border-blue-900">
          
          {/* Col 1: Brand & Credential Summary */}
          <div className="lg:col-span-5 space-y-4 text-left">
            <div className="flex items-center gap-4">
              <div className="w-14 h-14 rounded-2xl bg-[#0b1e3b] border-2 border-[#c8102e] flex items-center justify-center p-0.5 shadow-md overflow-hidden shrink-0">
                <img
                  src="/logo-emblem.png"
                  alt="Coach's Eye Bowling Lab Logo"
                  className="w-full h-full object-cover"
                />
              </div>
              <div>
                <span className="font-display text-xl font-extrabold text-white uppercase tracking-tight block leading-none">
                  Alfredo Quilarquez
                </span>
                <div className="text-xs font-bold text-[#c39d5e] uppercase mt-1">
                  Coach's Eye Bowling Lab • USBC Level 1 & PBA Member
                </div>

              </div>
            </div>



            <p className="text-xs sm:text-sm text-slate-200 leading-relaxed font-body">
              Preserving bowling as a true athletic discipline through 240fps slow-motion video analysis, 3-6-9 spare precision, and custom physical timing optimization.
            </p>

            <div className="flex flex-wrap items-center gap-2 pt-1">
              <span className="text-[10px] font-bold bg-white/10 text-white border border-white/20 px-2.5 py-1 rounded">
                USBC Level 1 Certified
              </span>
              <span className="text-[10px] font-bold bg-red-600/30 text-red-200 border border-red-500/40 px-2.5 py-1 rounded">
                PBA Regional Competitor
              </span>
              <span className="text-[10px] font-bold bg-emerald-600/30 text-emerald-200 border border-emerald-500/40 px-2.5 py-1 rounded">
                SafeSport Cleared
              </span>
            </div>
          </div>

          {/* Col 2: Fast Navigation Links */}
          <div className="lg:col-span-3 space-y-3 text-left">
            <div className="font-display font-bold text-sm text-white uppercase tracking-wider mb-2">
              Coaching & Portals
            </div>
            <ul className="space-y-2 text-xs font-semibold">
              <li>
                <button 
                  onClick={() => onNavClick('packages')}
                  className="text-slate-200 hover:text-amber-400 transition-colors flex items-center gap-1.5 cursor-pointer"
                >
                  <ChevronRight className="w-3 h-3 text-red-400" />
                  <span>Coaching Packages ($65+)</span>
                </button>
              </li>
              <li>
                <button 
                  onClick={() => onNavClick('about')}
                  className="text-slate-200 hover:text-amber-400 transition-colors flex items-center gap-1.5 cursor-pointer"
                >
                  <ChevronRight className="w-3 h-3 text-red-400" />
                  <span>About Coach Alfredo</span>
                </button>
              </li>
              <li>
                <button 
                  onClick={() => onNavClick('biomechanics')}
                  className="text-slate-200 hover:text-amber-400 transition-colors flex items-center gap-1.5 cursor-pointer"
                >
                  <ChevronRight className="w-3 h-3 text-red-400" />
                  <span>3-6-9 Spare Matrix & Oil Guide</span>
                </button>
              </li>
              <li>
                <button 
                  onClick={() => onNavClick('pro-shop')}
                  className="text-slate-200 hover:text-amber-400 transition-colors flex items-center gap-1.5 cursor-pointer"
                >
                  <ChevronRight className="w-3 h-3 text-red-400" />
                  <span>Pro Approach & Axis Point Partners</span>
                </button>
              </li>
              <li>
                <button 
                  onClick={() => onNavClick('faq')}
                  className="text-slate-200 hover:text-amber-400 transition-colors flex items-center gap-1.5 cursor-pointer"
                >
                  <ChevronRight className="w-3 h-3 text-red-400" />
                  <span>FAQ & Facility Location</span>
                </button>
              </li>
              <li>
                <button 
                  onClick={onOpenDiagnostic}
                  className="text-amber-300 font-bold hover:underline transition-colors flex items-center gap-1.5 cursor-pointer"
                >
                  <ChevronRight className="w-3 h-3 text-amber-400" />
                  <span>Preliminary Diagnostic Form</span>
                </button>
              </li>
            </ul>
          </div>

          {/* Col 3: Location & Clinic Updates */}
          <div className="lg:col-span-4 space-y-4 text-left">
            <div className="font-display font-bold text-sm text-white uppercase tracking-wider mb-2">
              Primary Location & Inquiries
            </div>

            <div className="space-y-2 text-xs">
              <div className="flex items-start gap-2.5 text-slate-200">
                <MapPin className="w-4 h-4 text-[#c8102e] shrink-0 mt-0.5" />
                <div>
                  <div className="font-bold text-white">{COACH_INFO.locationName}</div>
                  <div className="text-slate-300">{COACH_INFO.address}</div>
                </div>
              </div>

              <div className="flex items-center gap-2.5 text-slate-200">
                <Phone className="w-4 h-4 text-amber-400 shrink-0" />
                <span>{COACH_INFO.phone}</span>
              </div>

              <div className="flex items-center gap-2.5 text-slate-200">
                <Mail className="w-4 h-4 text-amber-400 shrink-0" />
                <a href={`mailto:${COACH_INFO.email}`} className="hover:text-amber-300">
                  {COACH_INFO.email}
                </a>
              </div>
            </div>

            {/* Newsletter for upcoming cohort clinic openings */}
            <div className="pt-2">
              <div className="text-xs text-slate-200 mb-1.5 font-semibold">
                Join Camp & Clinic Notification List:
              </div>
              {!emailSubscribed ? (
                <form onSubmit={handleNewsletter} className="flex gap-2">
                  <input
                    type="email"
                    required
                    value={newsletterEmail}
                    onChange={(e) => setNewsletterEmail(e.target.value)}
                    placeholder="Enter your email"
                    className="bg-white/10 border border-white/20 text-xs px-3 py-2.5 rounded-lg text-white w-full focus:bg-white focus:text-slate-900 focus:outline-none"
                  />
                  <button
                    type="submit"
                    className="btn-usbc-primary text-white font-bold px-4 py-2.5 rounded-lg text-xs shrink-0 flex items-center gap-1 cursor-pointer shadow-md"
                  >
                    <Send className="w-3 h-3" />
                    <span>Join</span>
                  </button>
                </form>
              ) : (
                <div className="text-xs text-emerald-300 flex items-center gap-1.5 bg-emerald-950/60 p-2.5 rounded border border-emerald-500/40 font-bold">
                  <CheckCircle2 className="w-4 h-4" />
                  <span>You're on the West Covina camp waitlist!</span>
                </div>
              )}
            </div>

          </div>

        </div>

        {/* Bottom Legal Bar */}
        <div className="pt-8 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-slate-300">
          <div>
            © {new Date().getFullYear()} Alfredo Quilarquez Bowling. All Rights Reserved.
          </div>

          <div className="flex items-center gap-4 font-semibold">
            <span>Bowlero West Covina, CA</span>
            <span>•</span>
            <span>USBC Level 1 & PBA Member</span>
          </div>
        </div>

      </div>
    </footer>
  );
};
