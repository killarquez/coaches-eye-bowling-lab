import React, { useState } from 'react';
import { 
  Calendar as CalendarIcon, 
  Plus, 
  Clock, 
  MapPin, 
  X, 
  ExternalLink, 
  Download, 
  User
} from 'lucide-react';
import type { CrmBooking, AthleteProfile } from '../../types/crm';
import { generateGoogleCalendarUrl, downloadIcsFile } from '../../services/crmStorage';

interface CrmCalendarViewProps {
  bookings: CrmBooking[];
  athletes: AthleteProfile[];
  onAddBooking: (booking: Partial<CrmBooking>) => void;
  onUpdateBookingStatus: (bookingId: string, status: CrmBooking['status']) => void;
  onSelectAthlete: (athleteId: string) => void;
}

export const CrmCalendarView: React.FC<CrmCalendarViewProps> = ({
  bookings,
  athletes,
  onAddBooking,
  onUpdateBookingStatus,
  onSelectAthlete
}) => {
  const [viewMode, setViewMode] = useState<'month' | 'list'>('month');
  const [selectedBooking, setSelectedBooking] = useState<CrmBooking | null>(null);
  const [showAddModal, setShowAddModal] = useState<boolean>(false);

  // New Booking Form State
  const [newAthleteId, setNewAthleteId] = useState<string>(athletes[0]?.id || '');
  const [newPackageName, setNewPackageName] = useState<string>('60-Minute Mechanical Tune-Up');
  const [newDate, setNewDate] = useState<string>('2026-09-02');
  const [newTime, setNewTime] = useState<string>('6:00 PM - 7:00 PM');
  const [newLane, setNewLane] = useState<string>('Lanes 19-20');
  const [newPrice, setNewPrice] = useState<number>(65);
  const [newNotes, setNewNotes] = useState<string>('');

  const daysOfWeek = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  
  const calendarDays = [
    { day: 30, month: 'prev', dateStr: '2026-08-30' },
    { day: 31, month: 'prev', dateStr: '2026-08-31' },
    { day: 1, month: 'current', dateStr: '2026-09-01' },
    { day: 2, month: 'current', dateStr: '2026-09-02' },
    { day: 3, month: 'current', dateStr: '2026-09-03' },
    { day: 4, month: 'current', dateStr: '2026-09-04' },
    { day: 5, month: 'current', dateStr: '2026-09-05' },
    { day: 6, month: 'current', dateStr: '2026-09-06' },
    { day: 7, month: 'current', dateStr: '2026-09-07' },
    { day: 8, month: 'current', dateStr: '2026-09-08' },
    { day: 9, month: 'current', dateStr: '2026-09-09' },
    { day: 10, month: 'current', dateStr: '2026-09-10' },
    { day: 11, month: 'current', dateStr: '2026-09-11' },
    { day: 12, month: 'current', dateStr: '2026-09-12' },
    { day: 13, month: 'current', dateStr: '2026-09-13' },
    { day: 14, month: 'current', dateStr: '2026-09-14' },
    { day: 15, month: 'current', dateStr: '2026-09-15' },
    { day: 16, month: 'current', dateStr: '2026-09-16' },
    { day: 17, month: 'current', dateStr: '2026-09-17' },
    { day: 18, month: 'current', dateStr: '2026-09-18' },
    { day: 19, month: 'current', dateStr: '2026-09-19' },
    { day: 20, month: 'current', dateStr: '2026-09-20' },
    { day: 21, month: 'current', dateStr: '2026-09-21' },
    { day: 22, month: 'current', dateStr: '2026-09-22' },
    { day: 23, month: 'current', dateStr: '2026-09-23' },
    { day: 24, month: 'current', dateStr: '2026-09-24' },
    { day: 25, month: 'current', dateStr: '2026-09-25' },
    { day: 26, month: 'current', dateStr: '2026-09-26' },
    { day: 27, month: 'current', dateStr: '2026-09-27' },
    { day: 28, month: 'current', dateStr: '2026-09-28' },
    { day: 29, month: 'current', dateStr: '2026-09-29' },
    { day: 30, month: 'current', dateStr: '2026-09-30' },
    { day: 1, month: 'next', dateStr: '2026-10-01' },
    { day: 2, month: 'next', dateStr: '2026-10-02' },
    { day: 3, month: 'next', dateStr: '2026-10-03' }
  ];

  const getPackageBadgeStyle = (packageName: string) => {
    if (packageName.includes('5-Session') || packageName.includes('5-pack')) {
      return 'bg-red-50 text-[#c8102e] border-red-200';
    }
    if (packageName.includes('Clinic') || packageName.includes('Cohort') || packageName.includes('Camp')) {
      return 'bg-emerald-50 text-emerald-800 border-emerald-200';
    }
    if (packageName.includes('Arsenal') || packageName.includes('Ball')) {
      return 'bg-amber-50 text-amber-800 border-amber-200';
    }
    return 'bg-blue-50 text-[#00205b] border-blue-200';
  };

  const handleCreateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const athlete = athletes.find((a) => a.id === newAthleteId) || athletes[0];
    onAddBooking({
      athleteId: athlete.id,
      athleteName: athlete.fullName,
      athleteEmail: athlete.email,
      athletePhone: athlete.phone,
      packageName: newPackageName,
      date: newDate,
      timeSlot: newTime,
      laneAssignment: newLane,
      price: newPrice,
      focusNotes: newNotes || 'On-lane lesson at Bowlero West Covina',
      status: 'Confirmed',
      paymentStatus: 'Paid',
      location: 'Bowlero West Covina'
    });
    setShowAddModal(false);
  };

  return (
    <div className="space-y-6">
      
      {/* Calendar Header & Toolbar */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-white p-5 sm:p-6 rounded-2xl border border-slate-200 shadow-xs">
        <div>
          <div className="text-xs font-bold uppercase text-[#c8102e] tracking-wider flex items-center gap-1.5">
            <CalendarIcon className="w-3.5 h-3.5" />
            <span>Bowlero West Covina • Lane Schedule</span>
          </div>
          <h2 className="text-xl sm:text-2xl font-black font-display text-[#00205b] uppercase mt-0.5">
            September 2026
          </h2>
        </div>

        <div className="flex flex-wrap items-center gap-3 w-full sm:w-auto">
          <div className="flex items-center bg-slate-100 p-1 rounded-xl border border-slate-200 text-xs font-bold">
            <button
              onClick={() => setViewMode('month')}
              className={`px-3 py-1.5 rounded-lg transition-all cursor-pointer ${
                viewMode === 'month' ? 'bg-[#00205b] text-white shadow-xs' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              Month
            </button>
            <button
              onClick={() => setViewMode('list')}
              className={`px-3 py-1.5 rounded-lg transition-all cursor-pointer ${
                viewMode === 'list' ? 'bg-[#00205b] text-white shadow-xs' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              Agenda List ({bookings.length})
            </button>
          </div>

          <button
            onClick={() => setShowAddModal(true)}
            className="bg-[#c8102e] hover:bg-[#a60d24] text-white text-xs font-bold uppercase tracking-wider px-4 py-2.5 rounded-xl flex items-center gap-1.5 transition-all shadow-xs cursor-pointer border border-[#a60d24]"
          >
            <Plus className="w-4 h-4" />
            <span>Add Session</span>
          </button>
        </div>
      </div>

      {/* VIEW: Month Calendar Grid */}
      {viewMode === 'month' && (
        <div className="bg-white rounded-2xl border-2 border-slate-200 shadow-sm overflow-hidden">
          
          <div className="grid grid-cols-7 border-b border-slate-200 bg-slate-50 text-center text-xs font-bold text-slate-700 py-3 uppercase tracking-wider">
            {daysOfWeek.map((day, idx) => (
              <div key={idx} className={idx === 0 || idx === 6 ? 'text-slate-400' : ''}>
                {day}
              </div>
            ))}
          </div>

          <div className="grid grid-cols-7 auto-rows-fr divide-x divide-y divide-slate-100">
            {calendarDays.map((cell, idx) => {
              const dayBookings = bookings.filter((b) => b.date === cell.dateStr);
              const isCurrentMonth = cell.month === 'current';

              return (
                <div
                  key={idx}
                  className={`min-h-[110px] sm:min-h-[130px] p-2 flex flex-col justify-between transition-colors ${
                    isCurrentMonth ? 'bg-white hover:bg-slate-50/70' : 'bg-slate-50/50 text-slate-400'
                  }`}
                >
                  <div className="flex items-center justify-between mb-1">
                    <span className={`text-xs font-bold ${isCurrentMonth ? 'text-slate-800' : 'text-slate-400'}`}>
                      {cell.day}
                    </span>
                    {dayBookings.length > 0 && (
                      <span className="w-2 h-2 rounded-full bg-[#c8102e]" />
                    )}
                  </div>

                  <div className="space-y-1.5 overflow-hidden">
                    {dayBookings.map((b) => (
                      <button
                        key={b.id}
                        onClick={() => setSelectedBooking(b)}
                        className={`w-full text-left p-1.5 rounded-lg border text-[11px] font-bold leading-tight truncate block cursor-pointer transition-all hover:scale-[1.02] shadow-2xs ${getPackageBadgeStyle(b.packageName)}`}
                      >
                        <div className="truncate">{b.athleteName}</div>
                        <div className="text-[9px] opacity-80 truncate">{b.timeSlot}</div>
                      </button>
                    ))}
                  </div>

                  <div />
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* VIEW: Agenda List View */}
      {viewMode === 'list' && (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden divide-y divide-slate-100">
          <div className="p-4 bg-slate-50 border-b border-slate-200 flex items-center justify-between text-xs font-bold text-slate-700 uppercase">
            <span>Upcoming Sessions Schedule ({bookings.length} Total)</span>
            <span>Bowlero West Covina</span>
          </div>

          {bookings.map((b) => (
            <div
              key={b.id}
              className="p-5 sm:p-6 hover:bg-slate-50 transition-colors flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4"
            >
              <div className="space-y-1.5">
                <div className="flex flex-wrap items-center gap-2">
                  <span className={`px-2.5 py-1 rounded-md text-[10px] font-bold border uppercase ${getPackageBadgeStyle(b.packageName)}`}>
                    {b.packageName}
                  </span>
                  <span className={`px-2.5 py-0.5 rounded text-[10px] font-bold ${
                    b.status === 'Confirmed' ? 'bg-emerald-50 text-emerald-800' : 'bg-slate-100 text-slate-700'
                  }`}>
                    {b.status}
                  </span>
                  <span className="text-xs font-mono font-bold text-slate-500">
                    {b.laneAssignment || 'Lanes 17-18'}
                  </span>
                </div>

                <h4 className="font-display text-lg font-black text-[#00205b]">
                  {b.athleteName}
                </h4>

                <div className="flex flex-wrap items-center gap-3 text-xs text-slate-600 font-medium">
                  <span className="flex items-center gap-1">
                    <CalendarIcon className="w-3.5 h-3.5 text-[#c8102e]" />
                    <span>{b.date}</span>
                  </span>
                  <span>•</span>
                  <span className="flex items-center gap-1">
                    <Clock className="w-3.5 h-3.5 text-slate-400" />
                    <span>{b.timeSlot}</span>
                  </span>
                  <span>•</span>
                  <span className="flex items-center gap-1">
                    <MapPin className="w-3.5 h-3.5 text-slate-400" />
                    <span>{b.location}</span>
                  </span>
                </div>

                <p className="text-xs text-slate-600 italic">
                  Focus: {b.focusNotes}
                </p>
              </div>

              <div className="flex items-center gap-2 w-full sm:w-auto shrink-0">
                <button
                  onClick={() => setSelectedBooking(b)}
                  className="px-3.5 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-[#00205b] text-xs font-bold transition-all cursor-pointer"
                >
                  Details
                </button>

                <a
                  href={generateGoogleCalendarUrl(b)}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="px-3 py-2 rounded-xl bg-blue-50 hover:bg-blue-100 text-[#00205b] border border-blue-200 text-xs font-bold flex items-center gap-1 transition-all cursor-pointer"
                  title="Add to Google Calendar"
                >
                  <ExternalLink className="w-3.5 h-3.5 text-[#c8102e]" />
                  <span>G-Cal</span>
                </a>

                <button
                  onClick={() => downloadIcsFile(b)}
                  className="p-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 border border-slate-300 transition-all cursor-pointer"
                  title="Download .ics for Apple/Outlook"
                >
                  <Download className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* SESSION DETAIL MODAL */}
      {selectedBooking && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="relative w-full max-w-xl bg-white rounded-3xl shadow-2xl border-2 border-slate-300 overflow-hidden animate-in fade-in zoom-in-95">
            
            <div className="bg-[#00205b] text-white p-6 border-b-4 border-[#c8102e] flex items-center justify-between">
              <div>
                <span className="text-[10px] font-bold uppercase tracking-wider text-[#c39d5e]">
                  Booking ID: {selectedBooking.id}
                </span>
                <h3 className="font-display text-xl font-black uppercase text-white">
                  {selectedBooking.athleteName}
                </h3>
              </div>
              <button
                onClick={() => setSelectedBooking(null)}
                className="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="p-6 space-y-5 text-xs text-slate-700">
              
              <div className="grid grid-cols-2 gap-3">
                <div className="bg-slate-50 p-3 rounded-xl border border-slate-200">
                  <span className="text-slate-500 block text-[10px] uppercase font-bold">Package & Service</span>
                  <span className="font-bold text-[#00205b]">{selectedBooking.packageName}</span>
                </div>
                <div className="bg-slate-50 p-3 rounded-xl border border-slate-200">
                  <span className="text-slate-500 block text-[10px] uppercase font-bold">Session Fee</span>
                  <span className="font-bold text-[#c8102e]">${selectedBooking.price} ({selectedBooking.paymentStatus})</span>
                </div>
              </div>

              <div className="space-y-2 bg-blue-50/70 p-4 rounded-xl border border-blue-200">
                <div className="flex items-center gap-2 font-bold text-[#00205b]">
                  <CalendarIcon className="w-4 h-4 text-[#c8102e]" />
                  <span>{selectedBooking.date} @ {selectedBooking.timeSlot}</span>
                </div>
                <div className="flex items-center gap-2 text-slate-600">
                  <MapPin className="w-4 h-4 text-slate-400" />
                  <span>{selectedBooking.location} ({selectedBooking.laneAssignment || 'Lanes 17-18'})</span>
                </div>
              </div>

              <div>
                <span className="text-slate-500 block text-[10px] uppercase font-bold mb-1">Mechanical Focus Notes:</span>
                <p className="p-3 bg-slate-50 border border-slate-200 rounded-xl leading-relaxed text-slate-800">
                  {selectedBooking.focusNotes}
                </p>
              </div>

              <div>
                <span className="text-slate-500 block text-[10px] uppercase font-bold mb-1.5">Update Session Status:</span>
                <div className="grid grid-cols-3 gap-2">
                  {(['Confirmed', 'Completed', 'Cancelled'] as CrmBooking['status'][]).map((st) => (
                    <button
                      key={st}
                      onClick={() => {
                        onUpdateBookingStatus(selectedBooking.id, st);
                        setSelectedBooking({ ...selectedBooking, status: st });
                      }}
                      className={`py-2 px-3 rounded-lg text-xs font-bold border transition-all cursor-pointer ${
                        selectedBooking.status === st
                          ? 'bg-[#00205b] text-white border-[#00205b]'
                          : 'bg-white text-slate-700 border-slate-300 hover:bg-slate-100'
                      }`}
                    >
                      {st}
                    </button>
                  ))}
                </div>
              </div>

              <div className="pt-3 border-t border-slate-200 flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => {
                      onSelectAthlete(selectedBooking.athleteId);
                      setSelectedBooking(null);
                    }}
                    className="bg-[#00205b] hover:bg-[#001740] text-white font-bold text-xs uppercase px-4 py-2.5 rounded-xl flex items-center gap-1.5 transition-all shadow-xs cursor-pointer"
                  >
                    <User className="w-3.5 h-3.5" />
                    <span>View Bowler Profile</span>
                  </button>

                  <a
                    href={generateGoogleCalendarUrl(selectedBooking)}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs uppercase px-4 py-2.5 rounded-xl flex items-center gap-1.5 transition-all shadow-xs cursor-pointer"
                  >
                    <ExternalLink className="w-3.5 h-3.5" />
                    <span>G-Cal</span>
                  </a>
                </div>

                <button
                  onClick={() => downloadIcsFile(selectedBooking)}
                  className="bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs uppercase px-4 py-2.5 rounded-xl flex items-center gap-1.5 transition-all border border-slate-300 cursor-pointer"
                >
                  <Download className="w-3.5 h-3.5" />
                  <span>iCal</span>
                </button>
              </div>
            </div>

          </div>
        </div>
      )}

      {/* CREATE NEW WALK-IN / CUSTOM SESSION MODAL */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="relative w-full max-w-lg bg-white rounded-3xl shadow-2xl border-2 border-slate-300 overflow-hidden animate-in fade-in">
            
            <div className="bg-[#00205b] text-white p-6 border-b-4 border-[#c8102e] flex items-center justify-between">
              <h3 className="font-display text-xl font-black uppercase text-white">
                Add On-Lane Coaching Session
              </h3>
              <button
                onClick={() => setShowAddModal(false)}
                className="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleCreateSubmit} className="p-6 space-y-4 text-xs">
              
              <div>
                <label className="block font-bold text-slate-700 uppercase mb-1">Select Student Athlete *</label>
                <select
                  value={newAthleteId}
                  onChange={(e) => setNewAthleteId(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-300 rounded-xl p-3 text-xs text-slate-900 font-medium"
                >
                  {athletes.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.fullName} ({a.bookAverage} Avg • {a.style})
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-bold text-slate-700 uppercase mb-1">Date *</label>
                  <input
                    type="date"
                    required
                    value={newDate}
                    onChange={(e) => setNewDate(e.target.value)}
                    className="w-full bg-slate-50 border border-slate-300 rounded-xl p-3 text-xs text-slate-900"
                  />
                </div>
                <div>
                  <label className="block font-bold text-slate-700 uppercase mb-1">Time Slot *</label>
                  <select
                    value={newTime}
                    onChange={(e) => setNewTime(e.target.value)}
                    className="w-full bg-slate-50 border border-slate-300 rounded-xl p-3 text-xs text-slate-900"
                  >
                    <option value="5:00 PM - 6:00 PM">5:00 PM - 6:00 PM</option>
                    <option value="6:00 PM - 7:00 PM">6:00 PM - 7:00 PM</option>
                    <option value="7:00 PM - 8:00 PM">7:00 PM - 8:00 PM</option>
                    <option value="7:00 PM - 8:30 PM">7:00 PM - 8:30 PM (Clinic)</option>
                    <option value="8:00 PM - 9:00 PM">8:00 PM - 9:00 PM</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-bold text-slate-700 uppercase mb-1">Package Type</label>
                  <select
                    value={newPackageName}
                    onChange={(e) => {
                      setNewPackageName(e.target.value);
                      if (e.target.value.includes('5-Session')) setNewPrice(275);
                      else if (e.target.value.includes('Clinic')) setNewPrice(150);
                      else setNewPrice(65);
                    }}
                    className="w-full bg-slate-50 border border-slate-300 rounded-xl p-3 text-xs text-slate-900"
                  >
                    <option value="60-Minute Mechanical Tune-Up">60-Minute Tune-Up ($65)</option>
                    <option value="5-Session Progressive Blueprint">5-Session Blueprint ($275)</option>
                    <option value="4-Week Group Cohort Clinic">4-Week Group Clinic ($150)</option>
                    <option value="On-Lane Arsenal & Ball Dynamics">Arsenal Dynamics ($85)</option>
                  </select>
                </div>

                <div>
                  <label className="block font-bold text-slate-700 uppercase mb-1">Lane Assignment</label>
                  <input
                    type="text"
                    value={newLane}
                    onChange={(e) => setNewLane(e.target.value)}
                    placeholder="e.g. Lanes 19-20"
                    className="w-full bg-slate-50 border border-slate-300 rounded-xl p-3 text-xs text-slate-900"
                  />
                </div>
              </div>

              <div>
                <label className="block font-bold text-slate-700 uppercase mb-1">Mechanical Focus / Objectives</label>
                <textarea
                  rows={2}
                  value={newNotes}
                  onChange={(e) => setNewNotes(e.target.value)}
                  placeholder="e.g. 240fps video review on foul line knee flexion & 10-pin spare adjustments"
                  className="w-full bg-slate-50 border border-slate-300 rounded-xl p-3 text-xs text-slate-900"
                />
              </div>

              <div className="pt-4 border-t border-slate-200 flex justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-4 py-2.5 rounded-xl border border-slate-300 text-slate-700 font-bold text-xs uppercase"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="bg-[#c8102e] hover:bg-[#a60d24] text-white font-bold text-xs uppercase px-6 py-2.5 rounded-xl shadow-xs"
                >
                  Confirm & Schedule
                </button>
              </div>

            </form>

          </div>
        </div>
      )}

    </div>
  );
};
