import React, { useState } from 'react';
import { 
  Calendar as CalendarIcon, 
  Clock, 
  MapPin, 
  CheckCircle2, 
  ShieldCheck, 
  ChevronRight
} from 'lucide-react';
import confetti from 'canvas-confetti';
import { COACH_INFO, COACHING_PACKAGES } from '../data/coachingData';
import type { CoachingPackage, DiagnosticData } from '../types';

interface BookingSectionProps {
  selectedPackage?: CoachingPackage | null;
  diagnosticData?: DiagnosticData | null;
  onOpenDiagnostic: () => void;
}

export const BookingSection: React.FC<BookingSectionProps> = ({
  selectedPackage,
  diagnosticData,
  onOpenDiagnostic
}) => {
  const [activePackageId, setActivePackageId] = useState<string>(
    selectedPackage ? selectedPackage.id : 'tune-up'
  );
  const [selectedGroupTierIndex, setSelectedGroupTierIndex] = useState<number>(0);
  const [selectedDate, setSelectedDate] = useState<string>('Tomorrow, 6:00 PM');
  const [selectedTimeSlot, setSelectedTimeSlot] = useState<string>('6:00 PM - 7:00 PM');
  const [useCalendlyEmbed, setUseCalendlyEmbed] = useState<boolean>(false);
  const [bookingConfirmed, setBookingConfirmed] = useState<boolean>(false);

  const currentPkg = COACHING_PACKAGES.find((p) => p.id === activePackageId) || COACHING_PACKAGES[0];
  
  // Calculate dynamic price & duration for group clinic vs single packages
  const effectivePrice = currentPkg.id === 'group-camps' && currentPkg.groupTiers
    ? currentPkg.groupTiers[selectedGroupTierIndex].ratePerPerson
    : currentPkg.price;

  const effectiveDuration = currentPkg.id === 'group-camps' && currentPkg.groupTiers
    ? currentPkg.groupTiers[selectedGroupTierIndex].duration
    : currentPkg.duration;


  const availableDates = [
    { label: 'Wed, Aug 27', time: '5:30 PM - 6:30 PM', slotsLeft: 2 },
    { label: 'Thu, Aug 28', time: '6:00 PM - 7:00 PM', slotsLeft: 1 },
    { label: 'Sat, Aug 30', time: '10:00 AM - 11:00 AM', slotsLeft: 3 },
    { label: 'Sun, Aug 31', time: '1:00 PM - 2:00 PM', slotsLeft: 2 },
    { label: 'Tue, Sep 2', time: '6:30 PM - 7:30 PM', slotsLeft: 4 },
  ];

  const handleConfirmReservation = (e: React.FormEvent) => {
    e.preventDefault();
    setBookingConfirmed(true);
    try {
      confetti({
        particleCount: 120,
        spread: 80,
        origin: { y: 0.6 }
      });
    } catch {
      // fallback
    }
  };

  return (
    <div className="space-y-16">
      
      {/* View Switcher Toggle with Generous Spacing */}
      <div className="flex items-center justify-center gap-3">
        <button
          onClick={() => setUseCalendlyEmbed(false)}
          className={`px-6 py-3 rounded-xl text-xs font-bold uppercase tracking-wider transition-all cursor-pointer ${
            !useCalendlyEmbed
              ? 'bg-[#00205b] text-white shadow-md'
              : 'bg-white text-slate-700 hover:bg-slate-50 border border-slate-300'
          }`}
        >
          Direct Fast-Booking
        </button>
        <button
          onClick={() => setUseCalendlyEmbed(true)}
          className={`px-6 py-3 rounded-xl text-xs font-bold uppercase tracking-wider transition-all cursor-pointer ${
            useCalendlyEmbed
              ? 'bg-[#c8102e] text-white shadow-md'
              : 'bg-white text-slate-700 hover:bg-slate-50 border border-slate-300'
          }`}
        >
          Calendly Scheduler
        </button>
      </div>

      {/* Main Booking Container with Aligned Grid */}
      {!bookingConfirmed ? (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 lg:gap-12 items-start">
          
          {/* Left Column: Package Selector & Session Summary */}
          <div className="lg:col-span-5 space-y-8">
            
            {/* Package Selector Card */}
            <div className="card-usbc p-7 sm:p-8 border border-slate-200 space-y-5 shadow-sm">
              <div className="text-xs font-bold text-[#00205b] uppercase tracking-wider">
                1. Select Coaching Program:
              </div>

              <div className="space-y-3">
                {COACHING_PACKAGES.map((pkg) => {
                  const isSelected = pkg.id === activePackageId;
                  return (
                    <div key={pkg.id} className="space-y-2">
                      <button
                        type="button"
                        onClick={() => setActivePackageId(pkg.id)}
                        className={`w-full p-4.5 rounded-2xl text-left border transition-all flex items-center justify-between cursor-pointer ${
                          isSelected
                            ? 'bg-blue-50/90 border-[#00205b] ring-2 ring-blue-100'
                            : 'bg-white border-slate-200 text-slate-700 hover:bg-slate-50'
                        }`}
                      >
                        <div>
                          <div className="font-display font-bold text-sm sm:text-base text-[#00205b]">
                            {pkg.title}
                          </div>
                          <div className="text-xs text-slate-500 flex items-center gap-1.5 mt-1 font-medium">
                            <Clock className="w-3.5 h-3.5 text-[#c8102e]" />
                            <span>
                              {pkg.id === 'group-camps' && isSelected ? effectiveDuration : pkg.duration}
                            </span>
                          </div>
                        </div>

                        <div className="text-right">
                          <div className="font-display font-black text-xl text-[#00205b]">
                            {pkg.id === 'group-camps'
                              ? isSelected ? `$${effectivePrice}` : '$150–$180'
                              : `$${pkg.price}`}
                          </div>
                          <span className="text-[10px] font-semibold text-slate-500 block">
                            {pkg.id === 'group-camps' ? '/ person' : pkg.id === 'development-package' ? '/ 5 sessions' : '/ session'}
                          </span>
                        </div>
                      </button>

                      {/* Interactive Group Size Picker when 4-Week Group Clinic is Selected */}
                      {isSelected && pkg.groupTiers && (
                        <div className="bg-purple-50/90 border border-purple-200 p-3.5 rounded-2xl space-y-2 animate-in fade-in duration-150">
                          <div className="flex items-center justify-between text-xs font-bold text-[#00205b]">
                            <span>Select Your Group Size:</span>
                            <span className="text-[10px] text-[#c8102e] font-extrabold uppercase">Bigger Group = Lower Rate</span>
                          </div>
                          <div className="grid grid-cols-2 gap-1.5">
                            {pkg.groupTiers.map((tier, tIdx) => (
                              <button
                                key={tIdx}
                                type="button"
                                onClick={() => setSelectedGroupTierIndex(tIdx)}
                                className={`p-2 rounded-xl text-left border transition-all cursor-pointer ${
                                  selectedGroupTierIndex === tIdx
                                    ? 'bg-[#00205b] text-white border-[#00205b] shadow-xs'
                                    : 'bg-white text-slate-700 border-purple-100 hover:bg-purple-100/50'
                                }`}
                              >
                                <div className="text-xs font-bold">{tier.bowlers}</div>
                                <div className={`text-[11px] font-black ${selectedGroupTierIndex === tIdx ? 'text-amber-300' : 'text-[#c8102e]'}`}>
                                  ${tier.ratePerPerson} <span className="text-[9px] font-normal opacity-80">/ person</span>
                                </div>
                                <div className="text-[9px] opacity-75">{tier.duration.split('(')[0]}</div>
                              </button>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>

            </div>

            {/* Location & Diagnostic Check Badge */}
            <div className="card-usbc p-7 sm:p-8 border border-slate-200 space-y-5 text-xs shadow-sm">
              <div className="text-slate-900 font-bold uppercase flex items-center gap-2">
                <MapPin className="w-4 h-4 text-[#c8102e]" />
                <span>Facility & Location</span>
              </div>
              <div className="bg-slate-50 p-4.5 rounded-xl border border-slate-200 space-y-1">
                <div className="font-bold text-[#00205b] text-sm">{COACH_INFO.locationName}</div>
                <div className="text-slate-600">{COACH_INFO.address}</div>
                <div className="text-xs text-slate-500 pt-1 font-semibold">
                  Free parking • On-site pro shops available
                </div>
              </div>

              {diagnosticData ? (
                <div className="bg-emerald-50 border border-emerald-200 p-4 rounded-xl flex items-center justify-between text-emerald-900 font-semibold">
                  <span className="flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>Baseline & Signed Waiver: {diagnosticData.fullName}</span>
                  </span>
                  <span className="text-[10px] uppercase font-bold bg-emerald-100 px-2 py-0.5 rounded">SYNCED & WAIVER ON FILE</span>
                </div>

              ) : (
                <div className="bg-amber-50 border border-amber-200 p-4 rounded-xl flex items-center justify-between text-amber-900 font-semibold">
                  <span>Diagnostic assessment not yet filled</span>
                  <button
                    onClick={onOpenDiagnostic}
                    className="text-xs underline font-bold text-[#c8102e] cursor-pointer"
                  >
                    Fill Now →
                  </button>
                </div>
              )}
            </div>

          </div>

          {/* Right Column: Fast Booking Form or Calendly */}
          <div className="lg:col-span-7">
            {useCalendlyEmbed ? (
              /* Calendly Simulated Scheduler */
              <div className="card-usbc p-8 sm:p-10 border border-slate-200 shadow-md space-y-6">
                <div className="flex items-center justify-between border-b border-slate-100 pb-5">
                  <div>
                    <h3 className="font-display text-xl font-bold text-[#00205b] uppercase">
                      Official Calendly Integration
                    </h3>
                    <p className="text-xs text-slate-500 mt-0.5">
                      Select date and time slot from Coach Alfredo's official calendar
                    </p>
                  </div>
                  <span className="px-3 py-1 bg-blue-50 text-[#00205b] text-xs font-bold border border-blue-200 rounded-full">
                    LIVE SYNC
                  </span>
                </div>

                <div className="bg-slate-50 rounded-2xl p-6 sm:p-8 border border-slate-200 min-h-[420px] flex flex-col justify-between space-y-6">
                  <div className="space-y-5">
                    <div className="flex items-center gap-3.5">
                      <div className="w-12 h-12 rounded-full bg-[#00205b] text-white font-bold flex items-center justify-center font-display text-lg">
                        AQ
                      </div>
                      <div>
                        <div className="text-sm font-bold text-[#00205b]">Alfredo Quilarquez</div>
                        <div className="text-xs text-[#c8102e] font-semibold">{currentPkg.title} ({currentPkg.duration})</div>
                      </div>
                    </div>

                    {/* Date Picker Grid */}
                    <div className="grid grid-cols-5 gap-2.5 pt-2">
                      {availableDates.map((item, idx) => (
                        <button
                          key={idx}
                          onClick={() => {
                            setSelectedDate(item.label);
                            setSelectedTimeSlot(item.time);
                          }}
                          className={`p-3.5 rounded-xl text-center border transition-all cursor-pointer ${
                            selectedDate === item.label
                              ? 'bg-[#00205b] text-white border-[#00205b] font-bold shadow-xs'
                              : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-100'
                          }`}
                        >
                          <div className="text-[11px]">{item.label.split(',')[0]}</div>
                          <div className="text-xs font-bold mt-0.5">{item.label.split(',')[1]}</div>
                        </button>
                      ))}
                    </div>

                    <div className="pt-2">
                      <div className="text-xs font-bold text-slate-700 mb-2.5">Available Times for {selectedDate}:</div>
                      <div className="grid grid-cols-2 gap-2.5">
                        {['5:00 PM', '6:00 PM', '7:00 PM', '8:00 PM'].map((t) => (
                          <button
                            key={t}
                            onClick={() => setSelectedTimeSlot(`${t} - ${parseInt(t) + 1}:00 PM`)}
                            className={`p-3 rounded-xl text-xs font-bold border transition-all cursor-pointer ${
                              selectedTimeSlot.startsWith(t)
                                ? 'bg-[#c8102e] text-white border-[#c8102e]'
                                : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-100'
                            }`}
                          >
                            {t}
                          </button>
                        ))}
                      </div>
                    </div>
                  </div>

                  <div className="pt-6 border-t border-slate-200 flex justify-end">
                    <button
                      onClick={handleConfirmReservation}
                      className="bg-[#c8102e] hover:bg-[#a60d24] text-white font-bold text-xs uppercase tracking-wider px-7 py-4 rounded-xl flex items-center gap-2 cursor-pointer shadow-md border border-[#a60d24]"
                    >
                      <span>Schedule Lesson Now (${currentPkg.price})</span>
                      <ChevronRight className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>
            ) : (
              /* Direct On-Lane Fast Booking Form */
              <div className="card-usbc p-8 sm:p-10 border border-slate-200 shadow-md space-y-6">
                
                <div className="border-b border-slate-100 pb-5 flex items-center justify-between">
                  <div>
                    <div className="text-xs font-bold text-[#c8102e] uppercase">Step 2 of 2</div>
                    <h3 className="font-display text-2xl font-black text-[#00205b] uppercase">
                      Confirm Date & Session
                    </h3>
                  </div>
                  <div className="text-right">
                    <div className="font-display font-black text-3xl text-[#00205b]">${effectivePrice}</div>
                    <div className="text-[10px] font-semibold text-slate-400">
                      {currentPkg.id === 'group-camps' ? 'Per Person (4-Wk Clinic)' : 'Total at Booking'}
                    </div>
                  </div>
                </div>

                <form onSubmit={handleConfirmReservation} className="space-y-6">
                  {/* Time slot picker */}
                  <div>
                    <label className="block text-xs font-bold text-slate-700 uppercase mb-2.5">
                      Select Available Time Slot at Bowlero West Covina:
                    </label>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      {availableDates.map((item, idx) => (
                        <button
                          key={idx}
                          type="button"
                          onClick={() => {
                            setSelectedDate(item.label);
                            setSelectedTimeSlot(item.time);
                          }}
                          className={`p-4 rounded-xl text-left border transition-all flex items-center justify-between cursor-pointer ${
                            selectedDate === item.label
                              ? 'bg-blue-50/90 border-[#00205b] ring-2 ring-blue-100 text-[#00205b]'
                              : 'bg-slate-50 border-slate-200 text-slate-700 hover:bg-slate-100'
                          }`}
                        >
                          <div>
                            <div className="font-display font-bold text-sm text-[#00205b]">
                              {item.label}
                            </div>
                            <div className="text-xs font-semibold text-slate-500 mt-0.5">
                              {item.time}
                            </div>
                          </div>
                          <span className="text-[10px] font-bold text-slate-600 bg-white px-2.5 py-1 rounded border border-slate-200">
                            {item.slotsLeft} left
                          </span>
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Student Details */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-1">
                    <div>
                      <label className="block text-xs font-bold text-slate-700 uppercase mb-1.5">
                        Bowler / Group Lead Name *
                      </label>
                      <input
                        type="text"
                        required
                        defaultValue={diagnosticData?.fullName || ''}
                        placeholder="Your Full Name"
                        className="w-full bg-slate-50 border border-slate-300 rounded-xl py-3.5 px-4 text-sm text-slate-900 focus:border-[#00205b] focus:bg-white focus:outline-none"
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-700 uppercase mb-1.5">
                        Confirmation Email *
                      </label>
                      <input
                        type="email"
                        required
                        defaultValue={diagnosticData?.email || ''}
                        placeholder="your.email@domain.com"
                        className="w-full bg-slate-50 border border-slate-300 rounded-xl py-3.5 px-4 text-sm text-slate-900 focus:border-[#00205b] focus:bg-white focus:outline-none"
                      />
                    </div>
                  </div>

                  {/* Session Notes */}
                  <div>
                    <label className="block text-xs font-bold text-slate-700 uppercase mb-1.5">
                      Specific Mechanical Focus Notes
                    </label>
                    <textarea
                      rows={2}
                      defaultValue={diagnosticData ? `Goal: ${diagnosticData.primaryGoal}. Style: ${diagnosticData.bowlingStyle}` : ''}
                      placeholder="e.g. Struggling with corner 10-pins and 4-step footwork balance."
                      className="w-full bg-slate-50 border border-slate-300 rounded-xl py-3 px-4 text-xs text-slate-900 focus:border-[#00205b] focus:bg-white focus:outline-none font-sans"
                    />
                  </div>

                  {/* Final Button with Bold USBC Red */}
                  <div className="pt-2">
                    <button
                      type="submit"
                      className="w-full bg-[#c8102e] hover:bg-[#a60d24] text-white font-bold text-sm sm:text-base uppercase tracking-wider py-4.5 rounded-xl flex items-center justify-center gap-2 cursor-pointer shadow-lg hover:shadow-xl transition-all border border-[#a60d24]"
                    >
                      <CalendarIcon className="w-5 h-5" />
                      <span>Confirm Reservation for {selectedDate} (${effectivePrice}{currentPkg.id === 'group-camps' ? '/pp' : ''})</span>
                      <ChevronRight className="w-4 h-4" />
                    </button>
                  </div>

                  <div className="text-center text-xs text-slate-500 font-medium flex items-center justify-center gap-4 pt-1">
                    <span className="flex items-center gap-1.5 text-emerald-700 font-semibold">
                      <ShieldCheck className="w-4 h-4" />
                      <span>No upfront deposit required</span>
                    </span>
                    <span>•</span>
                    <span>Free 24h rescheduling</span>
                  </div>

                </form>
              </div>
            )}
          </div>

        </div>
      ) : (
        /* Booking Confirmation State */
        <div className="card-usbc p-10 sm:p-14 border-2 border-emerald-500 max-w-2xl mx-auto text-center space-y-6 shadow-xl animate-in zoom-in-95 duration-200">
          
          <div className="w-16 h-16 mx-auto rounded-2xl bg-emerald-50 border-2 border-emerald-500 text-emerald-600 flex items-center justify-center shadow-md">
            <CheckCircle2 className="w-8 h-8" />
          </div>

          <div className="space-y-2">
            <div className="inline-flex items-center gap-1.5 px-3.5 py-1 rounded-full bg-emerald-100 text-emerald-800 text-xs font-bold">
              <span>RESERVATION CONFIRMED</span>
            </div>
            <h3 className="font-display text-3xl font-black uppercase text-[#00205b]">
              You're On the Schedule!
            </h3>
            <p className="text-slate-600 text-sm font-body">
              A calendar invitation and pre-session preparation guide have been confirmed. Coach Alfredo looks forward to working on your physical game.
            </p>
          </div>

          {/* Session Detail Recap */}
          <div className="bg-slate-50 p-6 rounded-2xl border border-slate-200 text-left text-xs space-y-3">
            <div className="flex justify-between border-b border-slate-200 pb-2">
              <span className="text-slate-500">Coaching Program:</span>
              <span className="text-[#00205b] font-bold">{currentPkg.title} ({effectiveDuration})</span>
            </div>
            <div className="flex justify-between border-b border-slate-200 pb-2">
              <span className="text-slate-500">Date & Time:</span>
              <span className="text-[#c8102e] font-bold">{selectedDate} ({selectedTimeSlot})</span>
            </div>
            <div className="flex justify-between border-b border-slate-200 pb-2">
              <span className="text-slate-500">Location:</span>
              <span className="text-slate-800 font-bold">{COACH_INFO.locationName} (West Covina, CA)</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Session Fee:</span>
              <span className="text-emerald-700 font-bold text-sm">
                ${effectivePrice} {currentPkg.id === 'group-camps' ? '/ person (4-Week Clinic Series)' : ''}
              </span>
            </div>

          </div>

          {/* What to bring */}
          <div className="text-left bg-blue-50 border border-blue-200 p-5 rounded-xl text-xs space-y-1.5">
            <span className="font-bold text-[#00205b] uppercase block mb-1">
              What to bring on your lesson day:
            </span>
            <p className="text-slate-700">1. Your current bowling balls (we will verify layout and PAP).</p>
            <p className="text-slate-700">2. Bowling shoes and comfortable athletic clothing for video capture.</p>
            <p className="text-slate-700">3. Please arrive 10 minutes prior to warm up.</p>
          </div>

          <button
            onClick={() => setBookingConfirmed(false)}
            className="px-7 py-3.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-bold transition-all cursor-pointer"
          >
            ← Book Another Session or Make Changes
          </button>

        </div>
      )}

    </div>
  );
};
