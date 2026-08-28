import React, { useState } from 'react';
import { 
  Menu, 
  X, 
  ChevronRight, 
  Calendar, 
  Award, 
  MapPin, 
  Phone, 
  FileCheck
} from 'lucide-react';

import { COACH_INFO } from '../data/coachingData';

interface NavbarProps {
  activeTab: string;
  setActiveTab: (tab: string) => void;
  onOpenDiagnostic: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ activeTab, setActiveTab, onOpenDiagnostic }) => {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const navLinks = [
    { id: 'home', label: 'Home' },
    { id: 'about', label: 'About Coach Alfredo' },
    { id: 'packages', label: 'Coaching Packages' },
    { id: 'biomechanics', label: 'Biomechanics Lab' },
    { id: 'booking', label: 'Book & Diagnostic' },
    { id: 'locker', label: 'Bowler Locker' },
    { id: 'pro-shop', label: 'Pro Shop Partners' },
    { id: 'faq', label: 'FAQ & Location' },
  ];


  const handleNavClick = (id: string) => {
    setActiveTab(id);
    setMobileMenuOpen(false);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div className="sticky top-0 z-50 bg-white border-b border-slate-200 shadow-sm">
      
      {/* Top Clean White Utility Strip */}
      <div className="bg-slate-50 border-b border-slate-200 text-xs py-2.5 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-4">
          
          {/* Left info items - perfectly aligned */}
          <div className="flex items-center gap-4 sm:gap-6 flex-wrap">
            <span className="flex items-center gap-1.5 font-semibold text-slate-700">
              <MapPin className="w-4 h-4 text-[#c8102e] shrink-0" />
              <span>Training Center: <strong className="text-[#00205b] font-bold">{COACH_INFO.locationName}</strong> (West Covina, CA)</span>
            </span>

            <span className="hidden md:inline text-slate-300">•</span>

            <span className="hidden md:flex items-center gap-1.5 text-slate-700 font-semibold">
              <Award className="w-4 h-4 text-[#c39d5e] shrink-0" />
              <span>USBC Level 1 Certified • PBA Member</span>
            </span>
          </div>

          {/* Right info items - perfectly aligned */}
          <div className="flex items-center gap-4 sm:gap-6 text-slate-700">
            <button
              onClick={() => handleNavClick('locker')}
              className="text-xs font-bold text-[#00205b] hover:text-[#c8102e] flex items-center gap-1.5 cursor-pointer py-0.5 transition-colors"
            >
              <span className="w-2 h-2 rounded-full bg-emerald-500" />
              <span>Bowler Locker (OTP)</span>
            </button>

            <span className="text-slate-300">•</span>

            <button
              onClick={() => handleNavClick('admin')}
              className="text-xs font-bold text-slate-600 hover:text-[#00205b] flex items-center gap-1.5 cursor-pointer py-0.5 transition-colors"
            >
              <span>Coach CRM</span>
            </button>

            <span className="hidden lg:inline text-slate-300">•</span>

            <span className="hidden lg:flex items-center gap-1.5 font-medium">
              <Phone className="w-3.5 h-3.5 text-[#c8102e] shrink-0" />
              <span>Direct: <strong className="text-slate-900 font-bold">{COACH_INFO.phone}</strong></span>
            </span>
          </div>

        </div>
      </div>


      {/* Main Clean White Navigation Bar with Roomy Height and Exact Alignment */}
      <header className="bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-8 lg:gap-12 xl:gap-16 h-20 sm:h-22">
          
          {/* Brand Identity / Official Coach's Eye Bowling Lab Lockup with Gold Banner */}
          <button 
            onClick={() => handleNavClick('home')}
            className="flex items-center gap-3.5 text-left cursor-pointer focus:outline-none group shrink-0 mr-4 sm:mr-8 lg:mr-10"
          >
            {/* Official Coach's Eye Bowling Lab Emblem */}
            <div className="w-13 h-13 rounded-2xl bg-[#0b1e3b] border-2 border-[#c39d5e] flex items-center justify-center p-0.5 shadow-sm group-hover:scale-105 transition-all overflow-hidden shrink-0">
              <img
                src="/logo-emblem.png"
                alt="Coach's Eye Bowling Lab Emblem"
                className="w-full h-full object-cover"
              />
            </div>

            <div>
              <div className="flex items-center gap-2">
                <span className="font-logo font-black italic text-xl sm:text-2xl tracking-tight text-[#00205b] uppercase leading-none">
                  Coach's Eye
                </span>
                <span className="bg-[#c39d5e] text-white text-[10px] sm:text-[11px] font-black uppercase tracking-wider px-2 py-0.5 rounded shadow-xs font-logo italic leading-none">
                  Bowling Lab
                </span>
              </div>
              <div className="text-[10.5px] font-bold text-slate-500 tracking-wider uppercase mt-1 leading-tight">
                <div>Coach Alfredo Quilarquez</div>
                <div className="text-[#c8102e] font-extrabold">Biomechanics & Strategy</div>
              </div>
            </div>
          </button>






          {/* Desktop Navigation Links with generous spacing */}
          <nav className="hidden xl:flex items-center gap-2">

            {navLinks.map((link) => {
              const isActive = activeTab === link.id;
              return (
                <button
                  key={link.id}
                  onClick={() => handleNavClick(link.id)}
                  className={`px-4 py-2.5 text-sm font-bold rounded-xl transition-all cursor-pointer ${
                    isActive 
                      ? 'text-[#00205b] bg-blue-50/90 border-b-2 border-[#c8102e] shadow-xs' 
                      : 'text-slate-600 hover:text-[#00205b] hover:bg-slate-100/70'
                  }`}
                >
                  {link.label}
                </button>
              );
            })}
          </nav>

          {/* Desktop CTA Button */}
          <div className="hidden sm:flex items-center gap-3 shrink-0">
            <button
              onClick={() => handleNavClick('booking')}
              className="bg-[#c8102e] hover:bg-[#a60d24] text-white text-sm font-bold uppercase tracking-wider px-6 py-3.5 rounded-xl flex items-center gap-2 shadow-md hover:shadow-lg transition-all cursor-pointer border border-[#a60d24]"
            >
              <Calendar className="w-4 h-4" />
              <span>Book a Session</span>
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>

          {/* Mobile Hamburger Toggle */}
          <div className="flex xl:hidden items-center gap-3">
            <button
              onClick={() => handleNavClick('booking')}
              className="sm:hidden bg-[#c8102e] text-white text-xs font-bold uppercase px-3.5 py-2 rounded-lg shadow-sm"
            >
              Book Now
            </button>
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-label="Toggle Navigation Menu"
              className="p-2.5 rounded-xl bg-slate-100 text-slate-700 hover:bg-slate-200 border border-slate-300 cursor-pointer"
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>

        </div>
      </header>

      {/* Mobile Drawer */}
      {mobileMenuOpen && (
        <div className="xl:hidden border-t border-slate-200 bg-white p-6 space-y-4 shadow-2xl animate-in fade-in duration-150 max-h-[80vh] overflow-y-auto">
          <div className="text-xs font-bold uppercase tracking-wider text-slate-400 px-2 pb-2 border-b border-slate-100">
            Navigation Menu
          </div>
          <div className="space-y-1">
            {navLinks.map((link) => (
              <button
                key={link.id}
                onClick={() => handleNavClick(link.id)}
                className={`w-full text-left px-4 py-3 rounded-xl font-bold text-sm transition-colors flex items-center justify-between ${
                  activeTab === link.id
                    ? 'bg-[#00205b] text-white'
                    : 'text-slate-700 hover:bg-slate-100'
                }`}
              >
                <span>{link.label}</span>
                <ChevronRight className={`w-4 h-4 ${activeTab === link.id ? 'text-amber-400' : 'text-slate-400'}`} />
              </button>
            ))}
          </div>

          <div className="pt-4 border-t border-slate-200 space-y-3">
            <button
              onClick={() => {
                setMobileMenuOpen(false);
                onOpenDiagnostic();
              }}
              className="w-full py-3.5 px-4 rounded-xl bg-slate-100 hover:bg-slate-200 border border-slate-300 text-[#00205b] font-bold text-xs uppercase tracking-wider flex items-center justify-center gap-2"
            >
              <FileCheck className="w-4 h-4 text-[#c8102e]" />
              <span>Complete Preliminary Diagnostic</span>
            </button>

            <button
              onClick={() => handleNavClick('booking')}
              className="w-full bg-[#c8102e] hover:bg-[#a60d24] text-white py-4 px-4 rounded-xl font-bold text-xs uppercase tracking-wider flex items-center justify-center gap-2 shadow-md"
            >
              <Calendar className="w-4 h-4" />
              <span>Book a Session ($65+)</span>
            </button>
          </div>
        </div>
      )}

    </div>
  );
};
