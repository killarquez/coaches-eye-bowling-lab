import React, { useState } from 'react';
import { 
  ShieldCheck, 
  Lock, 
  Mail, 
  KeyRound, 
  ArrowRight, 
  Video, 
  CheckCircle2, 
  Calendar as CalendarIcon, 
  MapPin, 
  ExternalLink, 
  Download, 
  Folder, 
  Sparkles, 
  AlertCircle, 
  LogOut, 
  Disc,
  Play
} from 'lucide-react';
import type { AthleteProfile, CrmBooking } from '../../types/crm';

import { 
  getAthletes, 
  getBookings, 
  generateLockerOtp, 
  verifyLockerOtp,
  generateGoogleCalendarUrl,
  downloadIcsFile,
  fetchCloudAthletes,
  fetchCloudBookings
} from '../../services/crmStorage';

interface BowlerLockerProps {
  onClose?: () => void;
  initialEmail?: string;
}

export const BowlerLocker: React.FC<BowlerLockerProps> = ({
  initialEmail = ''
}) => {
  const [email, setEmail] = useState<string>(initialEmail);
  const [otpSent, setOtpSent] = useState<boolean>(false);
  const [otpCode, setOtpCode] = useState<string>('');
  const [generatedCode, setGeneratedCode] = useState<string>('');
  const [authenticatedAthlete, setAuthenticatedAthlete] = useState<AthleteProfile | null>(null);
  const [error, setError] = useState<string>('');
  const [activeTab, setActiveTab] = useState<'video' | 'drills' | 'arsenal' | 'schedule'>('video');

  const [athletes, setAthletes] = useState<AthleteProfile[]>(getAthletes());
  const [bookings, setBookings] = useState<CrmBooking[]>(getBookings());

  React.useEffect(() => {
    fetchCloudAthletes().then((data) => { if (data && data.length > 0) setAthletes(data); });
    fetchCloudBookings().then((data) => { if (data && data.length > 0) setBookings(data); });
  }, []);

  const handleRequestOtp = (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    const cleanEmail = email.trim().toLowerCase();
    
    if (!cleanEmail || !cleanEmail.includes('@')) {
      setError('Please enter a valid email address.');
      return;
    }

    const athlete = athletes.find((a) => a.email.toLowerCase() === cleanEmail);
    if (!athlete) {
      setError('No registered bowler record found with this email. Please complete the Diagnostic Assessment or enter Marcus/Elena sample email.');
      return;
    }

    const code = generateLockerOtp(cleanEmail);
    setGeneratedCode(code);
    setOtpSent(true);
  };

  const handleVerifyOtp = (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    const cleanEmail = email.trim().toLowerCase();

    if (!verifyLockerOtp(cleanEmail, otpCode)) {
      setError('Invalid or expired 6-digit verification code. (Hint: Use ' + generatedCode + ' or 123456)');
      return;
    }

    const athlete = athletes.find((a) => a.email.toLowerCase() === cleanEmail);
    if (athlete) {
      setAuthenticatedAthlete(athlete);
    }
  };

  const handleLogout = () => {
    setAuthenticatedAthlete(null);
    setOtpSent(false);
    setOtpCode('');
  };

  const athleteBookings = authenticatedAthlete
    ? bookings.filter((b) => b.athleteEmail.toLowerCase() === authenticatedAthlete.email.toLowerCase())
    : [];

  return (
    <div className="w-full max-w-6xl mx-auto space-y-6">
      
      {/* Top Banner */}
      <div className="bg-[#00205b] text-white rounded-3xl p-6 sm:p-8 border-b-4 border-[#c8102e] shadow-xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/10 text-[#c39d5e] border border-white/20 text-xs font-bold uppercase tracking-wider">
            <Lock className="w-3.5 h-3.5 text-[#c8102e]" />
            <span>Private Athlete Telemetry & Locker</span>
          </div>
          <h2 className="font-display text-2xl sm:text-3xl font-black uppercase tracking-tight text-white">
            Bowler Locker Room
          </h2>
          <p className="text-xs sm:text-sm text-slate-200 font-body">
            240fps Biomechanical Video Analysis • Coach Alfredo's Drill Prescriptions • Bowlero West Covina
          </p>
        </div>

        {authenticatedAthlete && (
          <button
            onClick={handleLogout}
            className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white/10 hover:bg-white/20 border border-white/20 text-xs font-bold text-white transition-all cursor-pointer"
          >
            <LogOut className="w-4 h-4" />
            <span>Sign Out</span>
          </button>
        )}
      </div>

      {/* NOT AUTHENTICATED: STEP 1 (Email Request) & STEP 2 (OTP Entry) */}
      {!authenticatedAthlete ? (
        <div className="max-w-md mx-auto card-usbc p-8 sm:p-10 rounded-3xl border-2 border-slate-200 shadow-xl bg-white space-y-6">
          
          <div className="text-center space-y-2">
            <div className="w-14 h-14 mx-auto rounded-2xl bg-blue-50 border border-blue-200 text-[#00205b] flex items-center justify-center shadow-xs">
              <KeyRound className="w-7 h-7 text-[#c8102e]" />
            </div>
            <h3 className="font-display text-xl font-black text-[#00205b] uppercase">
              {otpSent ? 'Enter 6-Digit Passcode' : 'Access Your Locker'}
            </h3>
            <p className="text-xs text-slate-600">
              {otpSent 
                ? `Enter the single-use verification code sent to ${email}`
                : 'Enter your registered email address to receive a secure 1-time verification code.'}
            </p>
          </div>

          {!otpSent ? (
            <form onSubmit={handleRequestOtp} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase mb-1.5">
                  Registered Email Address *
                </label>
                <div className="relative">
                  <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-3.5" />
                  <input
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="e.g. marcus.turner@example.com"
                    className="w-full bg-slate-50 border border-slate-300 rounded-xl py-3 pl-10 pr-4 text-sm text-slate-900 focus:border-[#00205b] focus:bg-white focus:outline-none"
                  />
                </div>
              </div>

              {error && (
                <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-xs font-bold text-red-600 flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{error}</span>
                </div>
              )}

              <button
                type="submit"
                className="w-full bg-[#c8102e] hover:bg-[#a60d24] text-white font-bold text-xs uppercase tracking-wider py-4 rounded-xl shadow-md transition-all flex items-center justify-center gap-2 cursor-pointer border border-[#a60d24]"
              >
                <span>Send One-Time Verification Code</span>
                <ArrowRight className="w-4 h-4" />
              </button>

              <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl text-[11px] text-slate-600 text-center">
                <strong>Quick Demo Access:</strong> Use <code className="bg-slate-200 px-1 py-0.5 rounded text-[#00205b] font-bold">marcus.turner@example.com</code> or <code className="bg-slate-200 px-1 py-0.5 rounded text-[#00205b] font-bold">elena.rodriguez@example.com</code>
              </div>
            </form>
          ) : (
            <form onSubmit={handleVerifyOtp} className="space-y-4">
              
              <div className="p-3.5 bg-emerald-50 border border-emerald-200 rounded-xl text-xs text-emerald-950 flex items-center justify-between">
                <div>
                  <span className="text-[10px] font-bold text-emerald-700 uppercase block">Simulated One-Time Code:</span>
                  <span className="font-mono text-lg font-black tracking-widest text-[#00205b]">{generatedCode || '123456'}</span>
                </div>
                <button
                  type="button"
                  onClick={() => setOtpCode(generatedCode || '123456')}
                  className="px-2.5 py-1 rounded bg-emerald-600 text-white font-bold text-[10px] uppercase hover:bg-emerald-700 cursor-pointer"
                >
                  Quick Fill
                </button>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase mb-1.5">
                  6-Digit Passcode *
                </label>
                <input
                  type="text"
                  maxLength={6}
                  required
                  autoFocus
                  value={otpCode}
                  onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, ''))}
                  placeholder="• • • • • •"
                  className="w-full text-center tracking-[0.5em] font-mono text-xl bg-slate-50 border border-slate-300 rounded-xl py-3 text-slate-900 focus:border-[#00205b] focus:bg-white focus:outline-none"
                />
              </div>

              {error && (
                <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-xs font-bold text-red-600 flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{error}</span>
                </div>
              )}

              <button
                type="submit"
                className="w-full bg-[#c8102e] hover:bg-[#a60d24] text-white font-bold text-xs uppercase tracking-wider py-4 rounded-xl shadow-md transition-all flex items-center justify-center gap-2 cursor-pointer border border-[#a60d24]"
              >
                <ShieldCheck className="w-4 h-4" />
                <span>Verify Passcode & Unlock Locker</span>
              </button>

              <button
                type="button"
                onClick={() => setOtpSent(false)}
                className="w-full text-center text-xs text-slate-500 hover:text-[#00205b] font-bold"
              >
                Change Email Address
              </button>
            </form>
          )}

        </div>
      ) : (
        /* AUTHENTICATED ATHLETE LOCKER DASHBOARD */
        <div className="space-y-6 animate-in fade-in zoom-in-95 duration-200">
          
          <div className="card-usbc p-6 sm:p-8 rounded-3xl border-2 border-slate-200 bg-white shadow-md space-y-6">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-100 pb-5">
              <div className="flex items-center gap-4">
                <div className="w-14 h-14 rounded-2xl bg-[#00205b] text-white font-display font-black text-xl flex items-center justify-center shadow-md">
                  {authenticatedAthlete.fullName.split(' ').map((n) => n[0]).join('')}
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-bold text-[#c8102e] uppercase font-mono tracking-wider">
                      {authenticatedAthlete.id}
                    </span>
                    <span className="text-[10px] font-bold bg-emerald-50 text-emerald-700 px-2 py-0.5 rounded border border-emerald-200">
                      Waiver Active
                    </span>
                  </div>
                  <h3 className="font-display text-2xl font-black text-[#00205b] uppercase">
                    {authenticatedAthlete.fullName}
                  </h3>
                  <p className="text-xs text-slate-500 font-medium">
                    {authenticatedAthlete.style} ({authenticatedAthlete.dominantHand}-Handed) • {authenticatedAthlete.packageTier}
                  </p>
                </div>
              </div>

              <div className="flex flex-wrap items-center gap-3 text-xs">
                <div className="bg-slate-50 p-3 rounded-xl border border-slate-200 text-center min-w-[90px]">
                  <span className="text-[10px] uppercase font-bold text-slate-400 block">Average</span>
                  <span className="font-black text-base text-[#00205b]">{authenticatedAthlete.bookAverage}</span>
                </div>
                <div className="bg-slate-50 p-3 rounded-xl border border-slate-200 text-center min-w-[90px]">
                  <span className="text-[10px] uppercase font-bold text-slate-400 block">High Series</span>
                  <span className="font-black text-base text-[#c8102e]">{authenticatedAthlete.careerHighSeries}</span>
                </div>
                <div className="bg-slate-50 p-3 rounded-xl border border-slate-200 text-center min-w-[90px]">
                  <span className="text-[10px] uppercase font-bold text-slate-400 block">Sessions</span>
                  <span className="font-black text-base text-emerald-700">
                    {authenticatedAthlete.sessionsCompleted} / {authenticatedAthlete.sessionsTotal}
                  </span>
                </div>
              </div>
            </div>

            <div className="flex items-center gap-2 border-b border-slate-200 pb-2 overflow-x-auto scrollbar-none">
              <button
                onClick={() => setActiveTab('video')}
                className={`px-4 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider flex items-center gap-2 transition-all cursor-pointer ${
                  activeTab === 'video' ? 'bg-[#00205b] text-white shadow-xs' : 'text-slate-600 hover:bg-slate-100'
                }`}
              >
                <Video className="w-3.5 h-3.5" />
                <span>240fps Video Analysis ({authenticatedAthlete.videos.length})</span>
              </button>

              <button
                onClick={() => setActiveTab('drills')}
                className={`px-4 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider flex items-center gap-2 transition-all cursor-pointer ${
                  activeTab === 'drills' ? 'bg-[#00205b] text-white shadow-xs' : 'text-slate-600 hover:bg-slate-100'
                }`}
              >
                <CheckCircle2 className="w-3.5 h-3.5" />
                <span>Drill Homework ({authenticatedAthlete.drills.length})</span>
              </button>

              <button
                onClick={() => setActiveTab('arsenal')}
                className={`px-4 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider flex items-center gap-2 transition-all cursor-pointer ${
                  activeTab === 'arsenal' ? 'bg-[#00205b] text-white shadow-xs' : 'text-slate-600 hover:bg-slate-100'
                }`}
              >
                <Disc className="w-3.5 h-3.5" />
                <span>Ball Arsenal & PAP Specs</span>
              </button>

              <button
                onClick={() => setActiveTab('schedule')}
                className={`px-4 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider flex items-center gap-2 transition-all cursor-pointer ${
                  activeTab === 'schedule' ? 'bg-[#00205b] text-white shadow-xs' : 'text-slate-600 hover:bg-slate-100'
                }`}
              >
                <CalendarIcon className="w-3.5 h-3.5" />
                <span>Schedule & G-Cal</span>
              </button>
            </div>

            {/* TAB CONTENT: 1. 240fps Video Analysis */}
            {activeTab === 'video' && (
              <div className="space-y-6">
                {authenticatedAthlete.videos.length > 0 ? (
                  authenticatedAthlete.videos.map((vid) => (
                    <div key={vid.id} className="bg-slate-50 border border-slate-200 rounded-2xl p-5 sm:p-6 space-y-4">
                      
                      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-200 pb-3">
                        <div className="flex items-center gap-2">
                          <span className="px-2.5 py-1 rounded bg-[#00205b] text-white font-bold text-[10px] uppercase">
                            {vid.fps} FPS Capture
                          </span>
                          <h4 className="font-display text-base font-bold text-[#00205b]">
                            {vid.title}
                          </h4>
                        </div>
                        <span className="text-xs text-slate-500 font-medium">Recorded: {vid.date}</span>
                      </div>

                      <div className="relative aspect-video max-h-[360px] bg-slate-900 rounded-xl overflow-hidden flex items-center justify-center group shadow-inner border border-slate-700">
                        <div className="text-center space-y-2 p-4">
                          <div className="w-14 h-14 mx-auto rounded-full bg-[#c8102e] text-white flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform">
                            <Play className="w-6 h-6 ml-1" />
                          </div>
                          <div className="text-white font-bold text-sm">240 FPS Biomechanical Telemetry Review</div>
                          <div className="text-slate-400 text-xs">Coach Alfredo Voiceover & Angle Breakdown</div>
                        </div>

                        <div className="absolute bottom-3 right-3">
                          <a
                            href={vid.url}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="bg-white/20 hover:bg-white/30 backdrop-blur-md text-white text-xs font-bold px-3 py-1.5 rounded-lg flex items-center gap-1.5"
                          >
                            <ExternalLink className="w-3 h-3" />
                            <span>Open in Fullscreen</span>
                          </a>
                        </div>
                      </div>

                      <div className="p-4 bg-blue-50 border border-blue-200 rounded-xl text-xs space-y-2">
                        <div className="font-bold text-[#00205b] uppercase flex items-center gap-1.5">
                          <Sparkles className="w-3.5 h-3.5 text-[#c8102e]" />
                          <span>Coach Alfredo's Mechanical Assessment:</span>
                        </div>
                        <p className="text-slate-800 leading-relaxed font-body">
                          {vid.coachingNotes}
                        </p>
                        <div className="pt-2 border-t border-blue-200 space-y-1">
                          <div className="font-bold text-[11px] text-[#00205b] uppercase">Key Checkpoints:</div>
                          {vid.keyCheckpoints.map((cp, idx) => (
                            <div key={idx} className="flex items-center gap-2 text-slate-700 text-[11px]">
                              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                              <span>{cp}</span>
                            </div>
                          ))}
                        </div>
                      </div>

                    </div>
                  ))
                ) : (
                  <div className="p-8 text-center bg-slate-50 rounded-2xl border border-slate-200 text-xs text-slate-600 space-y-2">
                    <Video className="w-8 h-8 mx-auto text-slate-400" />
                    <p className="font-bold text-slate-800">No 240fps video uploads yet for this bowler.</p>
                    <p>Your slow-motion video breakdowns will appear here right after your on-lane session at Bowlero West Covina!</p>
                  </div>
                )}
              </div>
            )}

            {/* TAB CONTENT: 2. Drill Prescriptions & Homework */}
            {activeTab === 'drills' && (
              <div className="space-y-4">
                {authenticatedAthlete.drills.length > 0 ? (
                  authenticatedAthlete.drills.map((drill) => (
                    <div
                      key={drill.id}
                      className="p-5 bg-white border border-slate-200 rounded-2xl space-y-2.5 shadow-xs"
                    >
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <div className="flex items-center gap-2">
                          <span className="px-2.5 py-0.5 rounded bg-blue-50 text-[#00205b] font-bold text-[10px] border border-blue-200 uppercase">
                            {drill.category}
                          </span>
                          <h4 className="font-display text-sm sm:text-base font-bold text-[#00205b]">
                            {drill.title}
                          </h4>
                        </div>
                        <span className="text-xs font-mono font-bold text-[#c8102e] bg-red-50 px-2.5 py-1 rounded border border-red-200">
                          {drill.targetReps}
                        </span>
                      </div>

                      <p className="text-xs text-slate-700 leading-relaxed font-body">
                        {drill.instructions}
                      </p>

                      <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-500">
                        <span className="flex items-center gap-1 text-emerald-700 font-semibold">
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          <span>Active Prescribed Drill</span>
                        </span>
                        <span>Practice before next session</span>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="p-8 text-center bg-slate-50 rounded-2xl border border-slate-200 text-xs text-slate-600 space-y-2">
                    <CheckCircle2 className="w-8 h-8 mx-auto text-slate-400" />
                    <p className="font-bold text-slate-800">No drill homework currently assigned.</p>
                    <p>Coach Alfredo assigns customized mechanical drills based on your high-speed video footage.</p>
                  </div>
                )}
              </div>
            )}

            {/* TAB CONTENT: 3. Ball Arsenal & PAP Specs */}
            {activeTab === 'arsenal' && (
              <div className="space-y-6">
                
                <div className="bg-slate-50 p-5 rounded-2xl border border-slate-200 grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
                  <div className="bg-white p-3 rounded-xl border border-slate-200">
                    <span className="text-slate-400 block text-[10px] uppercase font-bold">Positive Axis Point (PAP)</span>
                    <span className="font-bold text-[#00205b] text-sm">{authenticatedAthlete.papCoordinates || '4 1/2" Over, 1/2" Up'}</span>
                  </div>
                  <div className="bg-white p-3 rounded-xl border border-slate-200">
                    <span className="text-slate-400 block text-[10px] uppercase font-bold">Axis Tilt</span>
                    <span className="font-bold text-[#00205b] text-sm">{authenticatedAthlete.axisTiltDeg || 12}°</span>
                  </div>
                  <div className="bg-white p-3 rounded-xl border border-slate-200">
                    <span className="text-slate-400 block text-[10px] uppercase font-bold">Estimated Rev Rate</span>
                    <span className="font-bold text-[#c8102e] text-sm">{authenticatedAthlete.estimatedRevRateRpm || 420} RPM</span>
                  </div>
                  <div className="bg-white p-3 rounded-xl border border-slate-200">
                    <span className="text-slate-400 block text-[10px] uppercase font-bold">Average Ball Speed</span>
                    <span className="font-bold text-[#00205b] text-sm">{authenticatedAthlete.averageSpeedMph || 15.8} MPH</span>
                  </div>
                </div>

                <div className="space-y-3">
                  <h4 className="font-display text-sm font-bold text-[#00205b] uppercase">
                    Registered Arsenal Equipment & Layouts
                  </h4>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {authenticatedAthlete.arsenal.map((ball, idx) => (
                      <div key={idx} className="p-4 bg-white border border-slate-200 rounded-2xl space-y-1.5 shadow-xs text-xs">
                        <div className="flex items-center justify-between font-bold">
                          <span className="text-[#00205b] text-sm">{ball.name}</span>
                          <span className="text-slate-500 font-mono">{ball.weight}</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className="px-2 py-0.5 bg-slate-100 rounded text-[10px] font-bold text-slate-700">
                            {ball.coverstock}
                          </span>
                          {ball.layout && (
                            <span className="px-2 py-0.5 bg-red-50 text-[#c8102e] border border-red-200 rounded text-[10px] font-mono font-bold">
                              Layout: {ball.layout}
                            </span>
                          )}
                        </div>
                        {ball.notes && (
                          <p className="text-[11px] text-slate-600 italic">{ball.notes}</p>
                        )}
                      </div>
                    ))}
                  </div>
                </div>

              </div>
            )}

            {/* TAB CONTENT: 4. Schedule & Google Calendar */}
            {activeTab === 'schedule' && (
              <div className="space-y-6">
                
                <div className="p-5 bg-blue-50 border border-blue-200 rounded-2xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 text-xs">
                  <div className="space-y-1">
                    <div className="font-bold text-[#00205b] uppercase text-sm">
                      Next Confirmed On-Lane Coaching Session
                    </div>
                    <div className="flex items-center gap-2 text-slate-700 font-medium">
                      <CalendarIcon className="w-4 h-4 text-[#c8102e]" />
                      <span>{authenticatedAthlete.nextSessionDate || 'Sep 2, 2026'} @ {authenticatedAthlete.nextSessionTime || '6:00 PM - 7:00 PM'}</span>
                    </div>
                    <div className="flex items-center gap-2 text-slate-600">
                      <MapPin className="w-4 h-4 text-slate-400" />
                      <span>Bowlero West Covina (675 S Glendora Ave)</span>
                    </div>
                  </div>

                  {athleteBookings[0] && (
                    <div className="flex flex-wrap items-center gap-2">
                      <a
                        href={generateGoogleCalendarUrl(athleteBookings[0])}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs uppercase px-4 py-2.5 rounded-xl flex items-center gap-1.5 shadow-xs"
                      >
                        <ExternalLink className="w-3.5 h-3.5" />
                        <span>Add to Google Calendar</span>
                      </a>
                      <button
                        onClick={() => downloadIcsFile(athleteBookings[0])}
                        className="bg-white text-slate-700 border border-slate-300 font-bold text-xs uppercase px-3 py-2.5 rounded-xl flex items-center gap-1"
                      >
                        <Download className="w-3.5 h-3.5" />
                        <span>iCal</span>
                      </button>
                    </div>
                  )}
                </div>

                <div className="p-6 bg-slate-50 border border-slate-200 rounded-2xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 text-xs">
                  <div className="space-y-1">
                    <div className="font-bold text-[#00205b] uppercase flex items-center gap-1.5">
                      <Folder className="w-4 h-4 text-amber-500" />
                      <span>Personal Google Drive Archive Folder</span>
                    </div>
                    <p className="text-slate-600 text-xs">
                      All your raw 240fps video files, drill summary PDFs, and layout sheets are archived in your private Drive folder.
                    </p>
                  </div>

                  <a
                    href={authenticatedAthlete.googleDriveFolderUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="bg-[#00205b] hover:bg-[#001740] text-white font-bold text-xs uppercase px-5 py-3 rounded-xl flex items-center gap-1.5 shrink-0 shadow-xs cursor-pointer"
                  >
                    <ExternalLink className="w-3.5 h-3.5" />
                    <span>Open My Google Drive Folder</span>
                  </a>
                </div>

                <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-2xl flex items-center justify-between text-xs text-emerald-950">
                  <div className="flex items-center gap-2">
                    <ShieldCheck className="w-5 h-5 text-emerald-600 shrink-0" />
                    <div>
                      <span className="font-bold block">Certified Athletic Liability Waiver & SafeSport Cleared</span>
                      <span className="text-[11px] text-emerald-800">Timestamp: {authenticatedAthlete.signedWaiverTimestamp}</span>
                    </div>
                  </div>
                  <span className="px-2.5 py-1 rounded bg-emerald-700 text-white font-bold text-[10px] uppercase">
                    On File
                  </span>
                </div>

              </div>
            )}

          </div>

        </div>
      )}

    </div>
  );
};
