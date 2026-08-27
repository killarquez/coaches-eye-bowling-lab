import { useState } from 'react';
import { Navbar } from './components/Navbar';
import { HeroSection } from './components/HeroSection';
import { StatsBanner } from './components/StatsBanner';
import { MissionHook } from './components/MissionHook';
import { CoreServices } from './components/CoreServices';
import { AboutCoach } from './components/AboutCoach';
import { BiomechanicsLab } from './components/BiomechanicsLab';
import { PackagesPricing } from './components/PackagesPricing';
import { DiagnosticModal } from './components/DiagnosticModal';
import { DiagnosticForm } from './components/DiagnosticForm';
import { BookingSection } from './components/BookingSection';
import { ProShopPartners } from './components/ProShopPartners';
import { Testimonials } from './components/Testimonials';
import { FAQSection } from './components/FAQSection';
import { Footer } from './components/Footer';
import { MobileQuickBar } from './components/MobileQuickBar';
import type { CoachingPackage, DiagnosticData } from './types';
import { COACHING_PACKAGES } from './data/coachingData';
import { 
  Award, 
  MapPin, 
  Calendar, 
  ShieldCheck, 
  ChevronRight,
  Target,
  Sparkles
} from 'lucide-react';

export function App() {
  const [activeTab, setActiveTab] = useState<string>('home');
  const [isDiagnosticOpen, setIsDiagnosticOpen] = useState<boolean>(false);
  const [selectedPackage, setSelectedPackage] = useState<CoachingPackage | null>(COACHING_PACKAGES[0]);
  const [diagnosticData, setDiagnosticData] = useState<DiagnosticData | null>(null);

  const navigateToTab = (tab: string, pkg?: CoachingPackage) => {
    if (pkg) setSelectedPackage(pkg);
    setActiveTab(tab);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleOpenDiagnostic = (packageId?: string) => {
    if (packageId) {
      const pkg = COACHING_PACKAGES.find((p) => p.id === packageId);
      if (pkg) setSelectedPackage(pkg);
    }
    setIsDiagnosticOpen(true);
  };

  const handleProceedFromDiagnostic = (data: DiagnosticData) => {
    setDiagnosticData(data);
    setIsDiagnosticOpen(false);
    navigateToTab('booking');
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 font-sans selection:bg-[#c8102e] selection:text-white flex flex-col justify-between">
      
      {/* Clean White Sticky Header Navbar */}
      <Navbar
        activeTab={activeTab}
        setActiveTab={(tab) => navigateToTab(tab)}
        onOpenDiagnostic={() => handleOpenDiagnostic()}
      />

      {/* Dynamic Page Views with Generous Spacing & Aligned Layout */}
      <main className="flex-grow pb-20 md:pb-0">
        
        {/* ========================================================================= */}
        {/* PAGE 1: HOME (MAIN OVERVIEW) */}
        {/* ========================================================================= */}
        {activeTab === 'home' && (
          <div className="space-y-0 animate-in fade-in duration-200">
            {/* Hero Section */}
            <HeroSection
              onBookClick={() => navigateToTab('booking')}
              onViewPackages={() => navigateToTab('packages')}
              onOpenDiagnostic={() => handleOpenDiagnostic()}
              onLearnMore={() => navigateToTab('about')}
            />

            {/* Credibility Stats Banner */}
            <StatsBanner />

            {/* Mission Hook */}
            <MissionHook
              onLearnMore={() => navigateToTab('about')}
              onBookClick={() => navigateToTab('booking')}
            />

            {/* Featured Packages Preview */}
            <CoreServices
              onSelectPackage={(pkg) => navigateToTab('booking', pkg)}
              onOpenDiagnostic={(pkgId) => handleOpenDiagnostic(pkgId)}
              onViewAllPackages={() => navigateToTab('packages')}
            />

            {/* Verified Student Social Proof */}
            <Testimonials />

            {/* Quick Facility & Booking CTA Banner */}
            <section className="py-20 sm:py-24 bg-[#00205b] text-white">
              <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center space-y-8">
                <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-white/10 text-[#c39d5e] text-xs font-bold uppercase">
                  <MapPin className="w-4 h-4 text-[#c8102e]" />
                  <span>Bowlero West Covina • 675 S Glendora Ave</span>
                </div>

                
                <h3 className="font-display text-3xl sm:text-4xl lg:text-5xl font-black uppercase tracking-tight">
                  Ready to elevate your game with professional coaching?
                </h3>
                
                <p className="text-slate-200 max-w-2xl mx-auto text-base sm:text-lg">
                  Book a 60-minute quick tune-up or comprehensive 5-session progression with Coach Alfredo Quilarquez.
                </p>

                <div className="pt-4 flex flex-wrap items-center justify-center gap-5">
                  <button
                    onClick={() => navigateToTab('booking')}
                    className="bg-[#c8102e] hover:bg-[#a60d24] text-white font-bold text-sm sm:text-base uppercase tracking-wider px-8 py-4.5 rounded-xl flex items-center gap-2 shadow-xl hover:shadow-2xl transition-all cursor-pointer border border-[#a60d24]"
                  >
                    <Calendar className="w-5 h-5" />
                    <span>Book Your Session Now ($65+)</span>
                    <ChevronRight className="w-4 h-4" />
                  </button>

                  <button
                    onClick={() => handleOpenDiagnostic()}
                    className="bg-white/10 hover:bg-white/20 text-white font-bold text-sm sm:text-base px-7 py-4.5 rounded-xl border border-white/25 transition-all cursor-pointer"
                  >
                    <span>Complete Baseline Diagnostic</span>
                  </button>
                </div>
              </div>
            </section>
          </div>
        )}

        {/* ========================================================================= */}
        {/* PAGE 2: ABOUT COACH ALFREDO */}
        {/* ========================================================================= */}
        {activeTab === 'about' && (
          <div className="py-16 sm:py-20 lg:py-24 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-16 animate-in fade-in duration-200">
            
            {/* Page Header */}
            <div className="border-b border-slate-200 pb-10 text-left space-y-3">
              <div className="flex items-center gap-2 text-xs font-bold text-[#c8102e] uppercase">
                <Award className="w-4 h-4" />
                <span>Coach Profile & Methodology</span>
              </div>
              <h1 className="font-display text-4xl sm:text-5xl lg:text-6xl font-black text-[#00205b] uppercase tracking-tight">
                About Coach Alfredo Quilarquez
              </h1>
              <p className="text-slate-600 text-base sm:text-lg max-w-3xl pt-1">
                USBC Certified Level 1 Coach (Actively pursuing Bronze), PBA Regional Competitor, and biomechanics specialist based out of Bowlero West Covina.
              </p>
            </div>

            {/* Full Bio & 3-Pillar Philosophy */}
            <AboutCoach
              onBookClick={() => navigateToTab('booking')}
              onOpenDiagnostic={() => handleOpenDiagnostic()}
            />

          </div>
        )}

        {/* ========================================================================= */}
        {/* PAGE 3: COACHING PACKAGES & PRICING */}
        {/* ========================================================================= */}
        {activeTab === 'packages' && (
          <div className="py-16 sm:py-20 lg:py-24 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-16 animate-in fade-in duration-200">
            
            {/* Page Header */}
            <div className="border-b border-slate-200 pb-10 text-left space-y-3">
              <div className="flex items-center gap-2 text-xs font-bold text-[#c8102e] uppercase">
                <Target className="w-4 h-4" />
                <span>Instruction Options & Rates</span>
              </div>
              <h1 className="font-display text-4xl sm:text-5xl lg:text-6xl font-black text-[#00205b] uppercase tracking-tight">
                Coaching Packages & Pricing
              </h1>
              <p className="text-slate-600 text-base sm:text-lg max-w-3xl pt-1">
                All private and cohort training includes lane fees at Bowlero West Covina, 240fps slow-motion video capture, and personalized mechanical homework prescriptions.
              </p>
            </div>

            {/* Packages Grid & Full Comparison Matrix */}
            <PackagesPricing
              onSelectPackage={(pkg) => navigateToTab('booking', pkg)}
              onOpenDiagnostic={(pkgId) => handleOpenDiagnostic(pkgId)}
            />

          </div>
        )}

        {/* ========================================================================= */}
        {/* PAGE 4: BIOMECHANICS & STRATEGY LAB */}
        {/* ========================================================================= */}
        {activeTab === 'biomechanics' && (
          <div className="py-16 sm:py-20 lg:py-24 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-16 animate-in fade-in duration-200">
            
            {/* Page Header */}
            <div className="border-b border-slate-200 pb-10 text-left space-y-3">
              <div className="flex items-center gap-2 text-xs font-bold text-[#c8102e] uppercase">
                <Sparkles className="w-4 h-4" />
                <span>Technical Reference & Tools</span>
              </div>
              <h1 className="font-display text-4xl sm:text-5xl lg:text-6xl font-black text-[#00205b] uppercase tracking-tight">
                Biomechanics Lab & Lane Play Guide
              </h1>
              <p className="text-slate-600 text-base sm:text-lg max-w-3xl pt-1">
                Master the mathematical 3-6-9 spare conversion system, study real-world Kegel oil pattern cross-sections, and learn how to manage lane transition across 3-game league blocks.
              </p>
            </div>

            {/* Interactive Spare Calculator, Real Oil Graphs & 1H/2H Telemetry */}
            <BiomechanicsLab />

          </div>
        )}

        {/* ========================================================================= */}
        {/* PAGE 5: BOOK A SESSION & DIAGNOSTIC */}
        {/* ========================================================================= */}
        {activeTab === 'booking' && (
          <div className="py-16 sm:py-20 lg:py-24 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-16 animate-in fade-in duration-200">
            
            {/* Page Header */}
            <div className="border-b border-slate-200 pb-10 text-left space-y-3">
              <div className="flex items-center gap-2 text-xs font-bold text-[#c8102e] uppercase">
                <Calendar className="w-4 h-4" />
                <span>Bowlero West Covina Scheduling</span>
              </div>
              <h1 className="font-display text-4xl sm:text-5xl lg:text-6xl font-black text-[#00205b] uppercase tracking-tight">
                Book Your Coaching Session
              </h1>
              <p className="text-slate-600 text-base sm:text-lg max-w-3xl pt-1">
                Complete your preliminary baseline diagnostic and select your preferred on-lane time slot with Coach Alfredo Quilarquez.
              </p>
            </div>

            {/* Step 1 Diagnostic Form Section */}
            <div className="space-y-6">
              <div className="flex items-center gap-3">
                <span className="w-8 h-8 rounded-xl bg-[#00205b] text-white flex items-center justify-center font-black text-sm shadow-xs">
                  01
                </span>
                <div>
                  <div className="text-xs font-bold text-[#c8102e] uppercase">Phase One</div>
                  <h3 className="font-display text-xl sm:text-2xl font-black text-[#00205b] uppercase">
                    Preliminary Athlete Diagnostic
                  </h3>
                </div>
              </div>

              <DiagnosticForm
                onProceedToBooking={handleProceedFromDiagnostic}
              />
            </div>

            {/* Step 2 Booking Portal Section */}
            <div id="booking-view" className="space-y-6 pt-12 border-t-2 border-slate-200">
              <div className="flex items-center gap-3">
                <span className="w-8 h-8 rounded-xl bg-[#c8102e] text-white flex items-center justify-center font-black text-sm shadow-xs">
                  02
                </span>
                <div>
                  <div className="text-xs font-bold text-[#c8102e] uppercase">Phase Two</div>
                  <h3 className="font-display text-xl sm:text-2xl font-black text-[#00205b] uppercase">
                    Select On-Lane Date & Coaching Package
                  </h3>
                </div>
              </div>

              <BookingSection
                selectedPackage={selectedPackage}
                diagnosticData={diagnosticData}
                onOpenDiagnostic={() => handleOpenDiagnostic()}
              />
            </div>


          </div>
        )}

        {/* ========================================================================= */}
        {/* PAGE 6: PRO SHOP PARTNERS */}
        {/* ========================================================================= */}
        {activeTab === 'pro-shop' && (
          <div className="py-16 sm:py-20 lg:py-24 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-16 animate-in fade-in duration-200">
            
            {/* Page Header */}
            <div className="border-b border-slate-200 pb-10 text-left space-y-3">
              <div className="flex items-center gap-2 text-xs font-bold text-[#c8102e] uppercase">
                <ShieldCheck className="w-4 h-4" />
                <span>Local Pro Shop Network</span>
              </div>
              <h1 className="font-display text-4xl sm:text-5xl lg:text-6xl font-black text-[#00205b] uppercase tracking-tight">
                Trusted Pro Shop Partners
              </h1>
              <p className="text-slate-600 text-base sm:text-lg max-w-3xl pt-1">
                We work directly with certified IBPSIA master drillers in West Covina to ensure your ball layout, thumb pitch, and surface matching fit your natural release mechanics.
              </p>
            </div>

            {/* Pro Shop Partner Directory */}
            <ProShopPartners />

          </div>
        )}

        {/* ========================================================================= */}
        {/* PAGE 7: FAQ & LOCATION */}
        {/* ========================================================================= */}
        {activeTab === 'faq' && (
          <div className="py-16 sm:py-20 lg:py-24 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-16 animate-in fade-in duration-200">
            
            {/* Page Header */}
            <div className="border-b border-slate-200 pb-10 text-left space-y-3">
              <div className="flex items-center gap-2 text-xs font-bold text-[#c8102e] uppercase">
                <MapPin className="w-4 h-4" />
                <span>Facility & Frequently Asked Questions</span>
              </div>
              <h1 className="font-display text-4xl sm:text-5xl lg:text-6xl font-black text-[#00205b] uppercase tracking-tight">
                FAQ & Facility Location
              </h1>
              <p className="text-slate-600 text-base sm:text-lg max-w-3xl pt-1">
                Have questions before your first lesson? Check our answers below or reach out directly to Coach Alfredo.
              </p>
            </div>

            {/* FAQ Accordion & Facility Contact Card */}
            <FAQSection />

          </div>
        )}

      </main>

      {/* Global Footer */}
      <Footer
        onNavClick={(tab) => navigateToTab(tab)}
        onOpenDiagnostic={() => handleOpenDiagnostic()}
      />

      {/* Mobile Sticky Quick Action Bar */}
      <MobileQuickBar
        onBookClick={() => navigateToTab('booking')}
        onOpenDiagnostic={() => handleOpenDiagnostic()}
      />

      {/* Diagnostic Modal Popup */}
      <DiagnosticModal
        isOpen={isDiagnosticOpen}
        onClose={() => setIsDiagnosticOpen(false)}
        initialPackageId={selectedPackage?.id}
        onProceedToBooking={handleProceedFromDiagnostic}
      />

    </div>
  );
}

export default App;
