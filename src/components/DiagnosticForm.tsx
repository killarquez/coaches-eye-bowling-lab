import React, { useState } from 'react';
import { 
  ShieldCheck, 
  User, 
  Mail, 
  Phone, 
  Target, 
  CheckCircle2, 
  Sparkles, 
  RotateCcw,
  Calendar,
  ArrowRight,
  ArrowLeft,
  MapPin,
  Video,
  FileText,
  AlertTriangle
} from 'lucide-react';


import confetti from 'canvas-confetti';
import type { DiagnosticData } from '../types';
import { WaiverModal } from './WaiverModal';

interface DiagnosticFormProps {
  initialPackageId?: string;
  onProceedToBooking: (diagnosticData: DiagnosticData) => void;
  onClose?: () => void;
}

export const DiagnosticForm: React.FC<DiagnosticFormProps> = ({
  onProceedToBooking
}) => {
  const [formData, setFormData] = useState<DiagnosticData>({
    fullName: '',
    email: '',
    phone: '',
    currentAverage: '185',
    highSeries: '650',
    bowlingStyle: '2-Handed',
    dominantHand: 'Right',
    arsenalTypes: ['Solid Reactive', 'Pearl Reactive', 'Plastic Spare'],
    physicalLimitations: 'None',
    primaryGoal: 'Timing, Leverage & Foul Line Balance',
    preferredDays: ['Weekday Evenings', 'Weekend Mornings'],
    emergencyContactName: '',
    emergencyContactPhone: '',
    isMinor: false,
    parentGuardianName: '',
    liabilityConsent: false,
    videoConsent: true,
    marketingMediaConsent: 'granted',
    cancellationPolicyConsent: false,
    signedTimestamp: ''
  });

  const [step, setStep] = useState<number>(1);
  const [submitted, setSubmitted] = useState<boolean>(false);
  const [showWaiverModal, setShowWaiverModal] = useState<boolean>(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const ballOptions = [
    'Solid Reactive (Heavy Oil)',
    'Pearl Reactive (Backend Snap)',
    'Hybrid Reactive (Benchmark)',
    'Urethane (Friction Control)',
    'Plastic / Polyester (Spare Ball)'
  ];

  const goalOptions = [
    'Timing, Leverage & Foul Line Balance',
    '3-6-9 Single-Pin Spare Conversion',
    'Rev Rate & Ball Speed Synchronization (1H/2H)',
    'Reading Lane Oil Transition & Arsenal Moves',
    'Tournament Pressure & Mental Composure',
    'General Average Increase (+15-20 Pins)'
  ];

  const handleArsenalToggle = (type: string) => {
    setFormData((prev) => {
      const exists = prev.arsenalTypes.includes(type);
      if (exists) {
        return { ...prev, arsenalTypes: prev.arsenalTypes.filter((t) => t !== type) };
      } else {
        return { ...prev, arsenalTypes: [...prev.arsenalTypes, type] };
      }
    });
  };

  const validateStep1 = () => {
    const errs: Record<string, string> = {};
    if (!formData.fullName.trim()) errs.fullName = 'Full Name is required';
    if (!formData.email.trim() || !formData.email.includes('@')) errs.email = 'Valid email is required';
    if (!formData.phone.trim()) errs.phone = 'Phone number is required for booking text reminders';
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleNext = () => {
    if (step === 1) {
      if (validateStep1()) setStep(2);
    } else if (step === 2) {
      setStep(3);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (step === 1 && !validateStep1()) return;

    const validationErrors: Record<string, string> = {};
    if (!formData.liabilityConsent) {
      validationErrors.liability = 'You must accept the Assumption of Risk & Liability Release.';
    }
    if (!formData.cancellationPolicyConsent) {
      validationErrors.cancellation = 'You must agree to the 24-Hour Rescheduling/Cancellation Policy.';
    }
    if (formData.isMinor && !formData.parentGuardianName?.trim()) {
      validationErrors.minor = 'Parent / Guardian Legal Name is required for youth bowlers under 18.';
    }

    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    const timestamp = new Date().toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      hour12: true
    });

    setFormData((prev) => ({ ...prev, signedTimestamp: timestamp }));
    setSubmitted(true);
    try {
      confetti({
        particleCount: 100,
        spread: 70,
        origin: { y: 0.6 }
      });
    } catch {
      // fallback
    }
  };



  return (
    <div className="card-usbc rounded-3xl border-2 border-slate-300 shadow-xl overflow-hidden bg-white w-full">
      
      {/* Top USBC Navy Architectural Header */}
      <div className="bg-[#00205b] text-white p-7 sm:p-9 border-b-4 border-[#c8102e] text-center relative">
        <div className="inline-flex items-center gap-2 px-3.5 py-1 rounded-full bg-white/10 border border-white/20 text-[#c39d5e] text-xs font-bold uppercase tracking-wider mb-3 shadow-xs">
          <ShieldCheck className="w-4 h-4 text-[#c8102e]" />
          <span>Preliminary Baseline Assessment</span>
        </div>

        <h3 className="font-display text-2xl sm:text-3xl lg:text-4xl font-black uppercase tracking-tight text-white">
          {submitted ? 'Diagnostic Baseline Generated' : 'Bowler Diagnostic Evaluation'}
        </h3>
        
        <p className="text-xs sm:text-sm text-slate-200 font-body max-w-2xl mx-auto mt-2 leading-relaxed">
          {submitted 
            ? 'Review your custom mechanical diagnostic summary below before scheduling your session.'
            : 'Required before booking confirmation to tailor your on-lane session at Bowlero West Covina.'}
        </p>
      </div>

      {/* Step Indicator Progress Bar Strip */}
      {!submitted && (
        <div className="bg-slate-50 border-b border-slate-200 px-6 sm:px-10 py-4">
          <div className="flex items-center justify-between max-w-2xl mx-auto">
            
            {/* Step 1 Pill */}
            <div className="flex items-center gap-2.5">
              <span className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-black transition-colors ${
                step === 1 
                  ? 'bg-[#00205b] text-white ring-4 ring-blue-100 shadow-xs' 
                  : step > 1 
                    ? 'bg-emerald-600 text-white' 
                    : 'bg-slate-200 text-slate-600'
              }`}>
                {step > 1 ? <CheckCircle2 className="w-4 h-4" /> : '1'}
              </span>
              <span className={`text-xs font-extrabold uppercase tracking-wide ${step === 1 ? 'text-[#00205b]' : 'text-slate-600'}`}>
                1. Contact
              </span>
            </div>

            <div className="h-0.5 flex-1 mx-3 sm:mx-6 bg-slate-200" />

            {/* Step 2 Pill */}
            <div className="flex items-center gap-2.5">
              <span className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-black transition-colors ${
                step === 2 
                  ? 'bg-[#00205b] text-white ring-4 ring-blue-100 shadow-xs' 
                  : step > 2 
                    ? 'bg-emerald-600 text-white' 
                    : 'bg-slate-200 text-slate-600'
              }`}>
                {step > 2 ? <CheckCircle2 className="w-4 h-4" /> : '2'}
              </span>
              <span className={`text-xs font-extrabold uppercase tracking-wide ${step === 2 ? 'text-[#00205b]' : 'text-slate-600'}`}>
                2. Style & Avg
              </span>
            </div>

            <div className="h-0.5 flex-1 mx-3 sm:mx-6 bg-slate-200" />

            {/* Step 3 Pill */}
            <div className="flex items-center gap-2.5">
              <span className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-black transition-colors ${
                step === 3 
                  ? 'bg-[#00205b] text-white ring-4 ring-blue-100 shadow-xs' 
                  : 'bg-slate-200 text-slate-600'
              }`}>
                3
              </span>
              <span className={`text-xs font-extrabold uppercase tracking-wide ${step === 3 ? 'text-[#00205b]' : 'text-slate-600'}`}>
                3. Arsenal & Goals
              </span>
            </div>

          </div>
        </div>
      )}

      {/* Form Content Area with 12-Column Grid */}
      <div className="p-6 sm:p-10 lg:p-12">
        {!submitted ? (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-10 items-start">
            
            {/* Left 8 cols: Active Form */}
            <div className="lg:col-span-8">
              <form onSubmit={handleSubmit} className="space-y-6">



          {/* STEP 1: Student Information */}
          {step === 1 && (
            <div className="space-y-4 animate-in fade-in">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase mb-1">
                    Student Full Name *
                  </label>
                  <div className="relative">
                    <User className="w-4 h-4 text-slate-400 absolute left-3.5 top-3.5" />
                    <input
                      type="text"
                      required
                      value={formData.fullName}
                      onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
                      placeholder="e.g. Alfredo Quilárquez"
                      className="w-full bg-slate-50 border border-slate-300 rounded-xl py-3 pl-10 pr-4 text-sm text-slate-900 focus:border-[#00205b] focus:bg-white focus:outline-none"
                    />
                  </div>
                  {errors.fullName && <p className="text-xs text-red-600 mt-1 font-bold">{errors.fullName}</p>}
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase mb-1">
                    Email Address *
                  </label>
                  <div className="relative">
                    <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-3.5" />
                    <input
                      type="email"
                      required
                      value={formData.email}
                      onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                      placeholder="alfredo@example.com"
                      className="w-full bg-slate-50 border border-slate-300 rounded-xl py-3 pl-10 pr-4 text-sm text-slate-900 focus:border-[#00205b] focus:bg-white focus:outline-none"
                    />
                  </div>
                  {errors.email && <p className="text-xs text-red-600 mt-1 font-bold">{errors.email}</p>}
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase mb-1">
                  Phone Number (For West Covina Check-In & Reminders) *
                </label>
                <div className="relative">
                  <Phone className="w-4 h-4 text-slate-400 absolute left-3.5 top-3.5" />
                  <input
                    type="tel"
                    required
                    value={formData.phone}
                    onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                    placeholder="(909) 764-4824"
                    className="w-full bg-slate-50 border border-slate-300 rounded-xl py-3 pl-10 pr-4 text-sm text-slate-900 focus:border-[#00205b] focus:bg-white focus:outline-none"
                  />
                </div>

                {errors.phone && <p className="text-xs text-red-600 mt-1 font-bold">{errors.phone}</p>}
              </div>

              {/* STEP 1 MOVE ON ACTION BUTTON */}
              <div className="pt-6 border-t border-slate-200 flex flex-col sm:flex-row items-center justify-between gap-3">
                <span className="text-xs text-slate-500 font-medium">
                  Step 1 of 3: Contact Details
                </span>

                <button
                  type="button"
                  onClick={handleNext}
                  className="w-full sm:w-auto bg-[#c8102e] hover:bg-[#a60d24] text-white font-bold text-sm uppercase tracking-wider px-8 py-4 rounded-xl flex items-center justify-center gap-2 shadow-lg hover:shadow-xl transition-all cursor-pointer border border-[#a60d24]"
                >
                  <span>Next: Bowling Style & Averages</span>
                  <ArrowRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}

          {/* STEP 2: Style & Average Metrics */}
          {step === 2 && (
            <div className="space-y-5 animate-in fade-in">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                
                {/* Bowling Style */}
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase mb-1">
                    Bowling Style *
                  </label>
                  <div className="grid grid-cols-2 gap-2">
                    {['1-Handed', '2-Handed'].map((style) => (
                      <button
                        type="button"
                        key={style}
                        onClick={() => setFormData({ ...formData, bowlingStyle: style as any })}
                        className={`p-3 rounded-xl text-xs font-bold border transition-all cursor-pointer ${
                          formData.bowlingStyle === style
                            ? 'bg-[#00205b] text-white border-[#00205b] shadow-sm'
                            : 'bg-slate-50 text-slate-700 border-slate-300 hover:bg-slate-100'
                        }`}
                      >
                        {style}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Dominant Hand */}
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase mb-1">
                    Dominant Hand *
                  </label>
                  <div className="grid grid-cols-2 gap-2">
                    {['Right', 'Left'].map((hand) => (
                      <button
                        type="button"
                        key={hand}
                        onClick={() => setFormData({ ...formData, dominantHand: hand as any })}
                        className={`p-3 rounded-xl text-xs font-bold border transition-all cursor-pointer ${
                          formData.dominantHand === hand
                            ? 'bg-[#c8102e] text-white border-[#c8102e] shadow-sm'
                            : 'bg-slate-50 text-slate-700 border-slate-300 hover:bg-slate-100'
                        }`}
                      >
                        {hand}-Handed
                      </button>
                    ))}
                  </div>
                </div>

              </div>

              {/* League Average & High Series */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase mb-1">
                    Current Book / League Average
                  </label>
                  <input
                    type="number"
                    value={formData.currentAverage}
                    onChange={(e) => setFormData({ ...formData, currentAverage: e.target.value })}
                    placeholder="e.g. 185"
                    className="w-full bg-slate-50 border border-slate-300 rounded-xl py-3 px-4 text-sm text-slate-900 focus:border-[#00205b] focus:bg-white focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase mb-1">
                    Career High 3-Game Series
                  </label>
                  <input
                    type="number"
                    value={formData.highSeries}
                    onChange={(e) => setFormData({ ...formData, highSeries: e.target.value })}
                    placeholder="e.g. 650"
                    className="w-full bg-slate-50 border border-slate-300 rounded-xl py-3 px-4 text-sm text-slate-900 focus:border-[#00205b] focus:bg-white focus:outline-none"
                  />
                </div>
              </div>

              {/* Physical limitations */}
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase mb-1">
                  Physical Limitations, Knee/Wrist Injuries, or Flexibility Notes
                </label>
                <input
                  type="text"
                  value={formData.physicalLimitations}
                  onChange={(e) => setFormData({ ...formData, physicalLimitations: e.target.value })}
                  placeholder="e.g. Minor right knee stiffness; prefer 4-step approach"
                  className="w-full bg-slate-50 border border-slate-300 rounded-xl py-3 px-4 text-sm text-slate-900 focus:border-[#00205b] focus:bg-white focus:outline-none"
                />
              </div>

              {/* STEP 2 MOVE ON ACTION BUTTON */}
              <div className="pt-6 border-t border-slate-200 flex flex-col sm:flex-row items-center justify-between gap-3">
                <button
                  type="button"
                  onClick={() => setStep(1)}
                  className="text-xs font-bold text-slate-600 hover:text-[#00205b] flex items-center gap-1.5 cursor-pointer py-2.5 px-4 rounded-lg bg-slate-100 hover:bg-slate-200 transition-colors border border-slate-300"
                >
                  <ArrowLeft className="w-4 h-4" />
                  <span>Back to Step 1 (Contact)</span>
                </button>

                <button
                  type="button"
                  onClick={handleNext}
                  className="w-full sm:w-auto bg-[#c8102e] hover:bg-[#a60d24] text-white font-bold text-sm uppercase tracking-wider px-8 py-4 rounded-xl flex items-center justify-center gap-2 shadow-lg hover:shadow-xl transition-all cursor-pointer border border-[#a60d24]"
                >
                  <span>Next: Arsenal & Goals</span>
                  <ArrowRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}

          {/* STEP 3: Arsenal & Primary Coaching Goals */}
          {step === 3 && (
            <div className="space-y-5 animate-in fade-in">
              
              {/* Ball Arsenal Types */}
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase mb-2">
                  Current Arsenal / Ball Types Used (Select All That Apply)
                </label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  {ballOptions.map((opt) => (
                    <button
                      type="button"
                      key={opt}
                      onClick={() => handleArsenalToggle(opt.split(' ')[0] + ' ' + opt.split(' ')[1])}
                      className={`p-3 rounded-xl text-left text-xs font-bold flex items-center justify-between border transition-all cursor-pointer ${
                        formData.arsenalTypes.some(t => opt.includes(t))
                          ? 'bg-blue-50 text-[#00205b] border-[#00205b]'
                          : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100'
                      }`}
                    >
                      <span>{opt}</span>
                      {formData.arsenalTypes.some(t => opt.includes(t)) && (
                        <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 font-bold" />
                      )}
                    </button>
                  ))}
                </div>
              </div>

              {/* Primary Coaching Goal */}
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase mb-2">
                  Primary Coaching Goal for Session *
                </label>
                <select
                  value={formData.primaryGoal}
                  onChange={(e) => setFormData({ ...formData, primaryGoal: e.target.value })}
                  className="w-full bg-slate-50 border border-slate-300 rounded-xl py-3 px-4 text-sm text-slate-900 focus:border-[#00205b] focus:bg-white focus:outline-none"
                >
                  {goalOptions.map((goal, idx) => (
                    <option key={idx} value={goal}>
                      {goal}
                    </option>
                  ))}
                </select>
              </div>

              {/* COACH'S EYE BOWLING LAB 4-PART AGREEMENTS & CONSENT SUITE */}
              <div className="bg-slate-50 border-2 border-slate-300 rounded-2xl p-5 sm:p-7 space-y-6">
                <div className="flex flex-wrap items-center justify-between border-b border-slate-200 pb-3.5 gap-2">
                  <div className="flex items-center gap-2">
                    <ShieldCheck className="w-5 h-5 text-[#c8102e]" />
                    <div>
                      <span className="text-xs sm:text-sm font-bold text-[#00205b] uppercase font-display block">
                        Coach's Eye Bowling Lab Agreements & Consent
                      </span>
                      <span className="text-[11px] text-slate-500 font-body">Bowlero West Covina • Alfredo Quilarquez</span>
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => setShowWaiverModal(true)}
                    className="inline-flex items-center gap-1.5 text-xs font-bold text-[#c8102e] hover:underline cursor-pointer bg-red-50 border border-red-200 px-3 py-1.5 rounded-lg shadow-2xs"
                  >
                    <FileText className="w-3.5 h-3.5" />
                    <span>View All 4 Documents & Policies</span>
                  </button>
                </div>

                {/* 1. Assumption of Risk & Liability Release */}
                <div className="space-y-2 text-xs">
                  <div className="font-bold text-[#00205b] uppercase flex items-center gap-1.5">
                    <span className="w-4 h-4 rounded-full bg-[#00205b] text-white flex items-center justify-center text-[10px]">1</span>
                    <span>Assumption of Risk & Liability Release (Required)</span>
                  </div>
                  <label className="flex items-start gap-3 cursor-pointer bg-white p-3.5 rounded-xl border border-slate-200 hover:border-slate-300">
                    <input
                      type="checkbox"
                      checked={formData.liabilityConsent}
                      onChange={(e) => {
                        setFormData({ ...formData, liabilityConsent: e.target.checked });
                        if (e.target.checked && errors.liability) {
                          setErrors((prev) => {
                            const copy = { ...prev };
                            delete copy.liability;
                            return copy;
                          });
                        }
                      }}
                      className="w-4 h-4 mt-0.5 rounded border-slate-300 text-[#00205b] focus:ring-[#00205b] cursor-pointer shrink-0"
                    />
                    <span className="text-slate-800 leading-snug font-medium">
                      I acknowledge inherent physical risks (approach slip/trip hazards, repetitive motion, dropped equipment) and explicitly release Alfredo Quilarquez and Coach's Eye Bowling Lab from legal liability. I certify my medical fitness for on-lane drills and coaching.
                    </span>
                  </label>
                </div>

                {/* 2. Minor Bowler & SafeSport Acknowledgment */}
                <div className="space-y-2 text-xs">
                  <div className="font-bold text-[#00205b] uppercase flex items-center gap-1.5">
                    <span className="w-4 h-4 rounded-full bg-[#00205b] text-white flex items-center justify-center text-[10px]">2</span>
                    <span>Youth Bowler & SafeSport Verification</span>
                  </div>
                  
                  <label className="flex items-center gap-2 cursor-pointer text-slate-700 font-semibold mb-2">
                    <input
                      type="checkbox"
                      checked={formData.isMinor || false}
                      onChange={(e) => setFormData({ ...formData, isMinor: e.target.checked })}
                      className="w-4 h-4 rounded border-slate-300 text-[#00205b] focus:ring-[#00205b] cursor-pointer"
                    />
                    <span>Is the participating bowler under 18 years of age?</span>
                  </label>

                  {formData.isMinor && (
                    <div className="bg-emerald-50/70 border border-emerald-200 p-4 rounded-xl space-y-3 animate-in fade-in">
                      <div className="text-emerald-950 font-semibold text-[11px] leading-relaxed">
                        Coach Alfredo Quilarquez is USBC SafeSport and Registered Volunteer Program (RVP) cleared. All youth sessions adhere strictly to open-observation protocols.
                      </div>
                      <div>
                        <label className="block font-bold text-slate-800 uppercase text-[11px] mb-1">
                          Parent / Legal Guardian Full Name *
                        </label>
                        <input
                          type="text"
                          value={formData.parentGuardianName || ''}
                          onChange={(e) => setFormData({ ...formData, parentGuardianName: e.target.value })}
                          placeholder="Parent / Guardian Legal Name"
                          className="w-full bg-white border border-slate-300 rounded-lg p-2.5 text-xs text-slate-900 focus:border-[#00205b] focus:outline-none"
                        />
                      </div>
                    </div>
                  )}
                </div>

                {/* 3. Biomechanics & Video Media Release */}
                <div className="space-y-2 text-xs">
                  <div className="font-bold text-[#00205b] uppercase flex items-center gap-1.5">
                    <span className="w-4 h-4 rounded-full bg-[#00205b] text-white flex items-center justify-center text-[10px]">3</span>
                    <span>Biomechanics & Video Media Release</span>
                  </div>

                  <div className="bg-white p-3.5 rounded-xl border border-slate-200 space-y-2.5">
                    <div className="text-slate-600 leading-snug">
                      I consent to 240fps slow-motion video analysis for technique improvement. Please select your social media & promotional preference:
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1">
                      <label className={`p-2.5 rounded-lg border text-xs font-semibold flex items-start gap-2 cursor-pointer transition-all ${
                        formData.marketingMediaConsent === 'granted'
                          ? 'bg-blue-50 border-[#00205b] text-[#00205b]'
                          : 'bg-slate-50 border-slate-200 text-slate-700 hover:bg-slate-100'
                      }`}>
                        <input
                          type="radio"
                          name="marketingMedia"
                          checked={formData.marketingMediaConsent === 'granted'}
                          onChange={() => setFormData({ ...formData, marketingMediaConsent: 'granted' })}
                          className="mt-0.5 text-[#00205b] focus:ring-[#00205b]"
                        />
                        <span><strong>Grant Permission:</strong> May use anonymized/credited video clips on website or social media.</span>
                      </label>

                      <label className={`p-2.5 rounded-lg border text-xs font-semibold flex items-start gap-2 cursor-pointer transition-all ${
                        formData.marketingMediaConsent === 'private_only'
                          ? 'bg-blue-50 border-[#00205b] text-[#00205b]'
                          : 'bg-slate-50 border-slate-200 text-slate-700 hover:bg-slate-100'
                      }`}>
                        <input
                          type="radio"
                          name="marketingMedia"
                          checked={formData.marketingMediaConsent === 'private_only'}
                          onChange={() => setFormData({ ...formData, marketingMediaConsent: 'private_only' })}
                          className="mt-0.5 text-[#00205b] focus:ring-[#00205b]"
                        />
                        <span><strong>Private Only:</strong> Do NOT use footage outside of my private instructional review.</span>
                      </label>
                    </div>
                  </div>
                </div>

                {/* 4. Package Terms & Cancellation Policy */}
                <div className="space-y-2 text-xs">
                  <div className="font-bold text-[#00205b] uppercase flex items-center gap-1.5">
                    <span className="w-4 h-4 rounded-full bg-[#00205b] text-white flex items-center justify-center text-[10px]">4</span>
                    <span>Package Terms & 24-Hour Cancellation Policy (Required)</span>
                  </div>

                  <label className="flex items-start gap-3 cursor-pointer bg-white p-3.5 rounded-xl border border-slate-200 hover:border-slate-300">
                    <input
                      type="checkbox"
                      checked={formData.cancellationPolicyConsent}
                      onChange={(e) => {
                        setFormData({ ...formData, cancellationPolicyConsent: e.target.checked });
                        if (e.target.checked && errors.cancellation) {
                          setErrors((prev) => {
                            const copy = { ...prev };
                            delete copy.cancellation;
                            return copy;
                          });
                        }
                      }}
                      className="w-4 h-4 mt-0.5 rounded border-slate-300 text-[#00205b] focus:ring-[#00205b] cursor-pointer shrink-0"
                    />
                    <span className="text-slate-800 leading-snug font-medium">
                      I agree to provide at least <strong>24 hours' notice</strong> for rescheduling or cancellations. I understand prepaid multi-session packages/clinics remain valid for <strong>6 months</strong> and fees are non-refundable once sessions begin.
                    </span>
                  </label>
                </div>

                {/* Validation Errors Alert */}
                {(errors.liability || errors.cancellation || errors.minor) && (
                  <div className="p-3.5 rounded-xl bg-red-50 border border-red-200 text-xs font-bold text-red-600 space-y-1">
                    {errors.liability && <div className="flex items-center gap-2"><AlertTriangle className="w-4 h-4 shrink-0" /><span>{errors.liability}</span></div>}
                    {errors.cancellation && <div className="flex items-center gap-2"><AlertTriangle className="w-4 h-4 shrink-0" /><span>{errors.cancellation}</span></div>}
                    {errors.minor && <div className="flex items-center gap-2"><AlertTriangle className="w-4 h-4 shrink-0" /><span>{errors.minor}</span></div>}
                  </div>
                )}

                {/* Optional Emergency Contact */}
                <div className="pt-3 border-t border-slate-200 grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                  <div>
                    <label className="block font-bold text-slate-700 uppercase mb-1">
                      Emergency Contact Name (Optional)
                    </label>
                    <input
                      type="text"
                      value={formData.emergencyContactName}
                      onChange={(e) => setFormData({ ...formData, emergencyContactName: e.target.value })}
                      placeholder="e.g. Parent / Spouse Name"
                      className="w-full bg-white border border-slate-300 rounded-lg p-2.5 text-xs text-slate-900 focus:border-[#00205b] focus:outline-none"
                    />
                  </div>
                  <div>
                    <label className="block font-bold text-slate-700 uppercase mb-1">
                      Emergency Contact Phone (Optional)
                    </label>
                    <input
                      type="tel"
                      value={formData.emergencyContactPhone}
                      onChange={(e) => setFormData({ ...formData, emergencyContactPhone: e.target.value })}
                      placeholder="(909) 000-0000"
                      className="w-full bg-white border border-slate-300 rounded-lg p-2.5 text-xs text-slate-900 focus:border-[#00205b] focus:outline-none"
                    />
                  </div>
                </div>

              </div>

              {/* STEP 3 SUBMIT ACTION BUTTON */}
              <div className="pt-6 border-t border-slate-200 flex flex-col sm:flex-row items-center justify-between gap-3">
                <button
                  type="button"
                  onClick={() => setStep(2)}
                  className="text-xs font-bold text-slate-600 hover:text-[#00205b] flex items-center gap-1.5 cursor-pointer py-2.5 px-4 rounded-lg bg-slate-100 hover:bg-slate-200 transition-colors border border-slate-300"
                >
                  <ArrowLeft className="w-4 h-4" />
                  <span>Back to Step 2 (Style & Avg)</span>
                </button>

                <button
                  type="submit"
                  className="w-full sm:w-auto bg-[#c8102e] hover:bg-[#a60d24] text-white font-bold text-sm uppercase tracking-wider px-8 py-4 rounded-xl flex items-center justify-center gap-2 shadow-lg hover:shadow-xl transition-all cursor-pointer border border-[#a60d24]"
                >
                  <Sparkles className="w-4 h-4" />
                  <span>Generate Baseline Strategy</span>
                  <ArrowRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}
        </form>
      </div>



            {/* Right 4 cols: Side Info Panel with Pro Shop / Facility / Calibration Specs */}
            <div className="lg:col-span-4 space-y-4">
              <div className="bg-slate-50 border border-slate-200 rounded-2xl p-6 space-y-4 text-xs">
                <div className="font-display font-bold text-sm text-[#00205b] uppercase flex items-center gap-2">
                  <Video className="w-4 h-4 text-[#c8102e]" />
                  <span>Pre-Session Calibration</span>
                </div>
                <p className="text-slate-600 leading-relaxed font-body">
                  Coach Alfredo pre-calibrates 240fps slow-motion cameras and prepares customized drill progressions based on your style before you arrive.
                </p>
                <div className="space-y-2.5 pt-2 border-t border-slate-200 text-slate-700 font-medium">
                  <div className="flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>Saves 15+ minutes on-lane setup</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>Personalized video review attached</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>100% Free • No deposit required</span>
                  </div>
                </div>
              </div>

              <div className="bg-blue-50/80 border border-blue-200 rounded-2xl p-5 text-xs text-[#00205b] space-y-1.5">
                <div className="font-bold uppercase flex items-center gap-1.5">
                  <MapPin className="w-4 h-4 text-[#c8102e]" />
                  <span>Bowlero West Covina</span>
                </div>
                <p className="text-slate-600 text-[11px] leading-relaxed">
                  675 S Glendora Ave, West Covina, CA. Free parking and direct pro shop partner collaboration.
                </p>
              </div>
            </div>

          </div>
        ) : (
          /* Dynamic Assessment Summary Generated */
          <div className="space-y-6 animate-in zoom-in-95 duration-200 max-w-3xl mx-auto">

          
          <div className="bg-slate-50 rounded-2xl p-5 sm:p-6 border border-slate-200 space-y-4">
            <div className="flex flex-wrap items-center justify-between border-b border-slate-200 pb-3 gap-2">
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-500" />
                <span className="text-xs font-bold text-[#00205b] uppercase">
                  Athlete Diagnostic: {formData.fullName || 'Registered Student'}
                </span>
              </div>
              <span className="text-xs font-bold text-[#c8102e] bg-red-50 px-2.5 py-1 rounded border border-red-200">
                {formData.bowlingStyle} ({formData.dominantHand}-Handed)
              </span>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
              <div className="bg-white p-3 rounded-lg border border-slate-200">
                <span className="text-slate-500 block">Baseline Average</span>
                <span className="text-base font-bold text-[#00205b]">{formData.currentAverage} Pins</span>
              </div>
              <div className="bg-white p-3 rounded-lg border border-slate-200">
                <span className="text-slate-500 block">Career High Series</span>
                <span className="text-base font-bold text-[#c8102e]">{formData.highSeries}</span>
              </div>
              <div className="bg-white p-3 rounded-lg border border-slate-200 sm:col-span-2">
                <span className="text-slate-500 block">Target Focus Area</span>
                <span className="text-xs font-bold text-slate-900">{formData.primaryGoal}</span>
              </div>
            </div>

            {/* Signed Agreements Status Box */}
            <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl space-y-2 text-xs text-emerald-950">
              <div className="flex flex-wrap items-center justify-between font-bold border-b border-emerald-200 pb-2">
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                  <span>Coach's Eye Agreements On File</span>
                </div>
                <span className="text-emerald-700 text-[11px] font-mono font-medium">
                  Signed: {formData.signedTimestamp || 'Active'}
                </span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-[11px] font-medium pt-1 text-emerald-900">
                <div>✓ 1. Assumption of Risk & Liability Release: <span className="font-bold">Signed</span></div>
                <div>✓ 2. Package Terms & 24h Cancellation: <span className="font-bold">Agreed</span></div>
                <div>✓ 3. Video Media Preference: <span className="font-bold">{formData.marketingMediaConsent === 'granted' ? 'Promotional & Web' : 'Private Analysis Only'}</span></div>
                <div>✓ 4. SafeSport / Minor Status: <span className="font-bold">{formData.isMinor ? `Youth (Guardian: ${formData.parentGuardianName || 'Authorized'})` : 'Adult Athlete (18+)'}</span></div>
              </div>
            </div>


            {/* Coach's Automated Pre-Session Strategy Insight */}
            <div className="bg-blue-50 p-4 rounded-xl border border-blue-200 text-xs">
              <div className="font-bold text-[#00205b] uppercase mb-1 flex items-center gap-1.5">
                <Target className="w-4 h-4 text-[#c8102e]" />
                <span>Coach Alfredo's Pre-Session Strategy Note</span>
              </div>
              <p className="text-slate-700 leading-relaxed font-body">
                For a {formData.currentAverage}-average {formData.bowlingStyle} {formData.dominantHand.toLowerCase()}-handed bowler targeting <strong className="text-[#00205b]">{formData.primaryGoal}</strong>, we will calibrate your foul-line leverage and release angle in the first 15 minutes with 240fps video capture at Bowlero West Covina.
              </p>
            </div>
          </div>

          {/* Action to proceed into Scheduling */}
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pt-2 border-t border-slate-200">
            <button
              type="button"
              onClick={() => {
                setSubmitted(false);
                setStep(1);
              }}
              className="text-xs font-bold text-slate-600 hover:text-[#00205b] flex items-center gap-1.5 cursor-pointer py-2.5 px-4 rounded-lg bg-slate-100 hover:bg-slate-200 transition-colors border border-slate-300"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Edit Diagnostic Info</span>
            </button>

            <button
              type="button"
              onClick={() => onProceedToBooking(formData)}
              className="w-full sm:w-auto bg-[#c8102e] hover:bg-[#a60d24] text-white font-bold text-sm uppercase tracking-wider px-8 py-4 rounded-xl flex items-center justify-center gap-2 shadow-lg hover:shadow-xl transition-all cursor-pointer border border-[#a60d24]"
            >
              <Calendar className="w-4 h-4" />
              <span>Confirm & Choose Booking Slot ($65+)</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>

        </div>
      )}

      </div>

      {/* Global Waiver Modal */}
      <WaiverModal
        isOpen={showWaiverModal}
        onClose={() => setShowWaiverModal(false)}
        onAccept={() => {
          setFormData((prev) => ({
            ...prev,
            liabilityConsent: true,
            videoConsent: true
          }));
          setErrors((prev) => {
            const copy = { ...prev };
            delete copy.liability;
            return copy;
          });
        }}
      />
    </div>
  );
};

