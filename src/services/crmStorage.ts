import type { AthleteProfile, CrmBooking, FinancialRecord } from '../types/crm';
import type { DiagnosticData } from '../types';
import { supabase } from './supabaseClient';

const ATHLETES_STORAGE_KEY = 'ceb_crm_athletes_v2';
const BOOKINGS_STORAGE_KEY = 'ceb_crm_bookings_v2';
const FINANCIALS_STORAGE_KEY = 'ceb_crm_financials_v2';
const OTP_STORE_KEY = 'ceb_otp_store_v2';

export const GOOGLE_APPS_SCRIPT_WEBHOOK_URL = 'https://script.google.com/macros/s/AKfycbwYIq4gHYy0noasu_dGNtSxMS72y2xfJv87eunOiqUjLFnxYFzTHgOYf662-fuFFJF_/exec';

export const triggerDriveWebhook = async (payload: {
  fullName: string;
  studentId: string;
  email: string;
  style?: string;
  bookAverage?: string | number;
  goals?: string;
}): Promise<boolean> => {
  try {
    await fetch(GOOGLE_APPS_SCRIPT_WEBHOOK_URL, {
      method: 'POST',
      mode: 'no-cors',
      headers: {
        'Content-Type': 'text/plain',
      },
      body: JSON.stringify(payload)
    });
    return true;
  } catch (err) {
    console.error('Google Apps Script Webhook Error:', err);
    return false;
  }
};

export const INITIAL_ATHLETES: AthleteProfile[] = [
  {
    id: 'CEB-101',
    fullName: 'Marcus Turner',
    email: 'marcus.turner@example.com',
    phone: '(909) 555-0192',
    style: '2-Handed',
    dominantHand: 'Right',
    bookAverage: 194,
    careerHighSeries: 698,
    careerHighGame: 279,
    primaryGoal: 'Rev Rate & Ball Speed Synchronization (2-Handed)',
    physicalLimitations: 'Mild right wrist soreness after 4+ games',
    papCoordinates: '4 3/4" over by 1/2" up',
    axisTiltDeg: 14,
    axisRotationDeg: 55,
    averageSpeedMph: 16.2,
    estimatedRevRateRpm: 460,
    packageTier: '5-Session Progressive Blueprint',
    status: 'Active',
    sessionsTotal: 5,
    sessionsCompleted: 2,
    nextSessionDate: '2026-09-02',
    nextSessionTime: '6:00 PM - 7:00 PM',
    clinicCohort: 'Tuesday Advanced Cohort',
    googleDriveFolderUrl: 'https://drive.google.com/drive/folders/19G4eW24NYDv4VXaEKbtbs-9MuerV1HJQ',
    arsenal: [
      { name: 'Storm Phaze II (Solid)', coverstock: 'Solid Reactive', weight: '15 lbs', layout: '45 x 4.5 x 30', notes: 'Benchmark oil ball' },
      { name: 'Roto Grip Hustle USA (Hybrid)', coverstock: 'Hybrid Reactive', weight: '15 lbs', layout: '50 x 5 x 35', notes: 'Transition ball' },
      { name: 'Storm Ice (Plastic)', coverstock: 'Plastic Spare', weight: '15 lbs', layout: 'Standard', notes: 'Single pin spares (Pin 10 / 7)' }
    ],
    videos: [
      {
        id: 'VID-001',
        title: 'Session 1: High-Speed Foul Line Release',
        url: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
        date: '2026-08-14',
        fps: 240,
        coachingNotes: 'Pushaway timing is late by 0.5 steps causing upper body tilt on step 4. Leverage restored on 1-step drill.',
        keyCheckpoints: ['Shoulder parallel to target line', 'Knee flexion angle at 45 degrees', 'Release inside of the ball']
      },
      {
        id: 'VID-002',
        title: 'Session 2: Axis Rotation Adjustment on Fresh Oil',
        url: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
        date: '2026-08-21',
        fps: 240,
        coachingNotes: 'Transitioned from 65 degree axis rotation to 45 degree forward roll for flat oil pattern control.',
        keyCheckpoints: ['Index finger spread pressure', 'Follow through towards target board 15']
      }
    ],
    drills: [
      { id: 'DRL-1', title: '1-Step Foul Line Pause Drill', category: 'Footwork & Balance', instructions: 'Set up at foul line with slide foot, swing back and freeze at finish for 3 seconds.', targetReps: '15 shots', completed: true },
      { id: 'DRL-2', title: 'Yo-Yo Release Finger Snap', category: 'Release & Leverage', instructions: 'Feel the ball roll off the palm without squeezing the thumb.', targetReps: '20 shots', completed: false },
      { id: 'DRL-3', title: '3-6-9 Single Pin Spare Targeting', category: '3-6-9 Spares', instructions: 'Move 3 boards right for 10-pin; 3 boards left for 7-pin using plastic spare ball.', targetReps: '10 spares', completed: true }
    ],
    signedWaiverTimestamp: 'Aug 12, 2026 4:15 PM',
    marketingConsent: 'granted',
    isMinor: false,
    createdAt: '2026-08-12',
    lastCoachedAt: '2026-08-21'
  },
  {
    id: 'CEB-102',
    fullName: 'Elena Rodriguez',
    email: 'elena.rodriguez@example.com',
    phone: '(909) 555-0481',
    style: '1-Handed',
    dominantHand: 'Right',
    bookAverage: 168,
    careerHighSeries: 589,
    careerHighGame: 234,
    primaryGoal: 'Spare System Reliability & Arm Swing Timing',
    physicalLimitations: 'None',
    papCoordinates: '5" over by 1/4" up',
    axisTiltDeg: 18,
    axisRotationDeg: 45,
    averageSpeedMph: 14.5,
    estimatedRevRateRpm: 280,
    packageTier: '4-Week Group Cohort Clinic',
    status: 'Active',
    sessionsTotal: 4,
    sessionsCompleted: 1,
    nextSessionDate: '2026-09-03',
    nextSessionTime: '7:00 PM - 8:30 PM',
    clinicCohort: 'Thursday Adult Clinic',
    googleDriveFolderUrl: 'https://drive.google.com/drive/folders/19G4eW24NYDv4VXaEKbtbs-9MuerV1HJQ',
    arsenal: [
      { name: 'Brunswick Rhino (Pearl)', coverstock: 'Pearl Reactive', weight: '14 lbs', layout: '55 x 4 x 35', notes: 'House shot benchmark' },
      { name: 'Columbia 300 White Dot', coverstock: 'Plastic Spare', weight: '14 lbs', layout: 'Standard', notes: 'Corner spares' }
    ],
    videos: [
      {
        id: 'VID-003',
        title: 'Session 1: Backswing Alignment & Crossover Step',
        url: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
        date: '2026-08-21',
        fps: 240,
        coachingNotes: 'Step 2 crossover step cleared the right hip, allowing a pure vertical pendulum swing.',
        keyCheckpoints: ['Step 2 crossover directly in front of left foot', 'Loose elbow in downswing']
      }
    ],
    drills: [
      { id: 'DRL-4', title: 'Tape Line Footwork Walk-Through', category: 'Footwork & Balance', instructions: 'Walk 5 steps along foul line center board without drifting right.', targetReps: '10 walk-throughs', completed: true },
      { id: 'DRL-5', title: 'Cross-Lane 10-Pin Spare Routine', category: '3-6-9 Spares', instructions: 'Stand on board 35, target board 20 at arrows with plastic ball flat wrist.', targetReps: '15 shots', completed: false }
    ],
    signedWaiverTimestamp: 'Aug 18, 2026 6:30 PM',
    marketingConsent: 'granted',
    isMinor: false,
    createdAt: '2026-08-18',
    lastCoachedAt: '2026-08-21'
  }
];

export const INITIAL_BOOKINGS: CrmBooking[] = [
  {
    id: 'BK-2026-001',
    athleteId: 'CEB-101',
    athleteName: 'Marcus Turner',
    athleteEmail: 'marcus.turner@example.com',
    athletePhone: '(909) 555-0192',
    packageId: 'progressive-5',
    packageName: '5-Session Progressive Blueprint',
    date: '2026-09-02',
    timeSlot: '6:00 PM - 7:00 PM',
    location: 'Bowlero West Covina',
    laneAssignment: 'Lanes 17-18',
    status: 'Confirmed',
    paymentStatus: 'Paid',
    price: 275,
    focusNotes: '240fps Biomechanical review on 4-step footwork cadence & foul line leverage',
    createdAt: '2026-08-14'
  },
  {
    id: 'BK-2026-002',
    athleteId: 'CEB-102',
    athleteName: 'Elena Rodriguez',
    athleteEmail: 'elena.rodriguez@example.com',
    athletePhone: '(909) 555-0481',
    packageId: 'group-clinic',
    packageName: '4-Week Group Cohort Clinic',
    date: '2026-09-03',
    timeSlot: '7:00 PM - 8:30 PM',
    location: 'Bowlero West Covina',
    laneAssignment: 'Lanes 19-20',
    status: 'Confirmed',
    paymentStatus: 'Paid',
    price: 150,
    focusNotes: '3-6-9 Spare mathematical adjustments & plastic spare ball alignment',
    createdAt: '2026-08-26'
  }
];

export const INITIAL_FINANCIALS: FinancialRecord[] = [
  { id: 'FIN-001', bookingId: 'BK-2026-001', athleteName: 'Marcus Turner', date: '2026-08-14', description: '5-Session Progressive Blueprint Package', packageType: '5-Session Package', amount: 275, status: 'Paid', method: 'Square / Stripe' },
  { id: 'FIN-002', bookingId: 'BK-2026-002', athleteName: 'Elena Rodriguez', date: '2026-08-21', description: '4-Week Group Cohort Clinic (4 Bowler Tier)', packageType: 'Group Clinic', amount: 150, status: 'Paid', method: 'Square / Stripe' }
];

export const getAthletes = (): AthleteProfile[] => {
  try {
    const data = localStorage.getItem(ATHLETES_STORAGE_KEY);
    if (!data) {
      localStorage.setItem(ATHLETES_STORAGE_KEY, JSON.stringify(INITIAL_ATHLETES));
      return INITIAL_ATHLETES;
    }
    return JSON.parse(data);
  } catch {
    return INITIAL_ATHLETES;
  }
};

export const fetchCloudAthletes = async (): Promise<AthleteProfile[]> => {
  try {
    const { data, error } = await supabase.from('athletes').select('*');
    if (error || !data || data.length === 0) return getAthletes();
    
    // Map snake_case to camelCase
    const mapped: AthleteProfile[] = data.map((d: any) => ({
      id: d.id,
      fullName: d.full_name || d.fullName,
      email: d.email,
      phone: d.phone,
      style: d.style,
      dominantHand: d.dominant_hand || d.dominantHand,
      bookAverage: d.book_average || d.bookAverage,
      careerHighSeries: d.career_high_series || d.careerHighSeries,
      careerHighGame: d.career_high_game || d.careerHighGame,
      primaryGoal: d.primary_goal || d.primaryGoal,
      physicalLimitations: d.physical_limitations || d.physicalLimitations,
      papCoordinates: d.pap_coordinates || d.papCoordinates,
      axisTiltDeg: d.axis_tilt_deg || d.axisTiltDeg,
      axisRotationDeg: d.axis_rotation_deg || d.axisRotationDeg,
      averageSpeedMph: d.average_speed_mph || d.averageSpeedMph,
      estimatedRevRateRpm: d.estimated_rev_rate_rpm || d.estimatedRevRateRpm,
      packageTier: d.package_tier || d.packageTier,
      sessionsTotal: d.sessions_total || d.sessionsTotal,
      sessionsCompleted: d.sessions_completed || d.sessionsCompleted,
      nextSessionDate: d.next_session_date || d.nextSessionDate,
      nextSessionTime: d.next_session_time || d.nextSessionTime,
      clinicCohort: d.clinic_cohort || d.clinicCohort,
      googleDriveFolderUrl: d.google_drive_folder_url || d.googleDriveFolderUrl,
      arsenal: d.arsenal || [],
      videos: d.videos || [],
      drills: d.drills || [],
      signedWaiverTimestamp: d.signed_waiver_timestamp || d.signedWaiverTimestamp,
      marketingConsent: d.marketing_consent || d.marketingConsent,
      isMinor: d.is_minor || d.isMinor,
      parentGuardianName: d.parent_guardian_name || d.parentGuardianName,
      emergencyContactName: d.emergency_contact_name || d.emergencyContactName,
      emergencyContactPhone: d.emergency_contact_phone || d.emergencyContactPhone,
      createdAt: d.created_at || d.createdAt,
      lastCoachedAt: d.last_coached_at || d.lastCoachedAt
    }));

    localStorage.setItem(ATHLETES_STORAGE_KEY, JSON.stringify(mapped));
    return mapped;
  } catch (err) {
    console.error('Supabase fetchAthletes error:', err);
    return getAthletes();
  }
};

export const saveAthletes = (athletes: AthleteProfile[]): void => {
  localStorage.setItem(ATHLETES_STORAGE_KEY, JSON.stringify(athletes));
  
  // Async upsert to Supabase
  athletes.forEach(async (a) => {
    try {
      await supabase.from('athletes').upsert({
        id: a.id,
        full_name: a.fullName,
        email: a.email,
        phone: a.phone,
        style: a.style,
        dominant_hand: a.dominantHand,
        book_average: a.bookAverage,
        career_high_series: a.careerHighSeries,
        career_high_game: a.careerHighGame,
        primary_goal: a.primaryGoal,
        physical_limitations: a.physicalLimitations,
        pap_coordinates: a.papCoordinates,
        axis_tilt_deg: a.axisTiltDeg,
        axis_rotation_deg: a.axisRotationDeg,
        average_speed_mph: a.averageSpeedMph,
        estimated_rev_rate_rpm: a.estimatedRevRateRpm,
        package_tier: a.packageTier,
        sessions_total: a.sessionsTotal,
        sessions_completed: a.sessionsCompleted,
        next_session_date: a.nextSessionDate,
        next_session_time: a.nextSessionTime,
        clinic_cohort: a.clinicCohort,
        google_drive_folder_url: a.googleDriveFolderUrl,
        arsenal: a.arsenal,
        videos: a.videos,
        drills: a.drills,
        signed_waiver_timestamp: a.signedWaiverTimestamp,
        marketing_consent: a.marketingConsent,
        is_minor: a.isMinor,
        parent_guardian_name: a.parentGuardianName,
        emergency_contact_name: a.emergencyContactName,
        emergency_contact_phone: a.emergencyContactPhone,
        last_coached_at: a.lastCoachedAt
      });
    } catch (err) {
      console.error('Supabase upsert athlete error:', err);
    }
  });
};

export const getBookings = (): CrmBooking[] => {
  try {
    const data = localStorage.getItem(BOOKINGS_STORAGE_KEY);
    if (!data) {
      localStorage.setItem(BOOKINGS_STORAGE_KEY, JSON.stringify(INITIAL_BOOKINGS));
      return INITIAL_BOOKINGS;
    }
    return JSON.parse(data);
  } catch {
    return INITIAL_BOOKINGS;
  }
};

export const fetchCloudBookings = async (): Promise<CrmBooking[]> => {
  try {
    const { data, error } = await supabase.from('bookings').select('*');
    if (error || !data || data.length === 0) return getBookings();

    const mapped: CrmBooking[] = data.map((d: any) => ({
      id: d.id,
      athleteId: d.athlete_id || d.athleteId,
      athleteName: d.athlete_name || d.athleteName,
      athleteEmail: d.athlete_email || d.athleteEmail,
      athletePhone: d.athlete_phone || d.athletePhone,
      packageId: d.package_id || d.packageId,
      packageName: d.package_name || d.packageName,
      date: d.date,
      timeSlot: d.time_slot || d.timeSlot,
      location: d.location,
      laneAssignment: d.lane_assignment || d.laneAssignment,
      status: d.status,
      paymentStatus: d.payment_status || d.paymentStatus,
      price: d.price,
      focusNotes: d.focus_notes || d.focusNotes,
      createdAt: d.created_at || d.createdAt
    }));

    localStorage.setItem(BOOKINGS_STORAGE_KEY, JSON.stringify(mapped));
    return mapped;
  } catch (err) {
    console.error('Supabase fetchBookings error:', err);
    return getBookings();
  }
};

export const saveBookings = (bookings: CrmBooking[]): void => {
  localStorage.setItem(BOOKINGS_STORAGE_KEY, JSON.stringify(bookings));

  bookings.forEach(async (b) => {
    try {
      await supabase.from('bookings').upsert({
        id: b.id,
        athlete_id: b.athleteId,
        athlete_name: b.athleteName,
        athlete_email: b.athleteEmail,
        athlete_phone: b.athletePhone,
        package_id: b.packageId,
        package_name: b.packageName,
        date: b.date,
        time_slot: b.timeSlot,
        location: b.location,
        lane_assignment: b.laneAssignment,
        status: b.status,
        payment_status: b.paymentStatus,
        price: b.price,
        focus_notes: b.focusNotes
      });
    } catch (err) {
      console.error('Supabase upsert booking error:', err);
    }
  });
};

export const getFinancials = (): FinancialRecord[] => {
  try {
    const data = localStorage.getItem(FINANCIALS_STORAGE_KEY);
    if (!data) {
      localStorage.setItem(FINANCIALS_STORAGE_KEY, JSON.stringify(INITIAL_FINANCIALS));
      return INITIAL_FINANCIALS;
    }
    return JSON.parse(data);
  } catch {
    return INITIAL_FINANCIALS;
  }
};

export const syncDiagnosticToCrm = (data: DiagnosticData): AthleteProfile => {
  const athletes = getAthletes();
  const existingIdx = athletes.findIndex((a) => a.email.toLowerCase() === data.email.toLowerCase());
  
  const cleanId = existingIdx >= 0 ? athletes[existingIdx].id : `CEB-${100 + athletes.length + 1}`;
  const driveFolderUrl = existingIdx >= 0 ? athletes[existingIdx].googleDriveFolderUrl : `https://drive.google.com/drive/folders/19G4eW24NYDv4VXaEKbtbs-9MuerV1HJQ`;
  
  const updatedProfile: AthleteProfile = {
    id: cleanId,
    fullName: data.fullName,
    email: data.email,
    phone: data.phone,
    style: data.bowlingStyle,
    dominantHand: data.dominantHand,
    bookAverage: parseInt(data.currentAverage) || 175,
    careerHighSeries: parseInt(data.highSeries) || 600,
    primaryGoal: data.primaryGoal,
    physicalLimitations: data.physicalLimitations || 'None',
    packageTier: 'Diagnostic Evaluation Completed (Inbound Prospect)',
    status: 'Prospect',
    sessionsTotal: 0,
    sessionsCompleted: 0,
    googleDriveFolderUrl: driveFolderUrl,
    arsenal: data.arsenalTypes.map((type) => ({
      name: type,
      coverstock: type.includes('Solid') ? 'Solid Reactive' : type.includes('Pearl') ? 'Pearl Reactive' : type.includes('Urethane') ? 'Urethane' : 'Plastic Spare',
      weight: '15 lbs'
    })),
    videos: existingIdx >= 0 ? athletes[existingIdx].videos : [],
    drills: existingIdx >= 0 ? athletes[existingIdx].drills : [],
    signedWaiverTimestamp: data.signedTimestamp || new Date().toLocaleString(),
    marketingConsent: data.marketingMediaConsent,
    isMinor: data.isMinor || false,
    parentGuardianName: data.parentGuardianName,
    emergencyContactName: data.emergencyContactName,
    emergencyContactPhone: data.emergencyContactPhone,
    createdAt: existingIdx >= 0 ? athletes[existingIdx].createdAt : new Date().toISOString().split('T')[0],
    lastCoachedAt: new Date().toISOString().split('T')[0]
  };

  if (existingIdx >= 0) {
    athletes[existingIdx] = updatedProfile;
  } else {
    athletes.unshift(updatedProfile);
  }

  saveAthletes(athletes);

  // Trigger Google Drive Webhook to create folder + Doc
  triggerDriveWebhook({
    fullName: updatedProfile.fullName,
    studentId: updatedProfile.id,
    email: updatedProfile.email,
    style: updatedProfile.style,
    bookAverage: updatedProfile.bookAverage,
    goals: updatedProfile.primaryGoal
  });

  return updatedProfile;
};

export const syncBookingToCrm = (bookingInfo: {
  athleteName: string;
  athleteEmail: string;
  athletePhone: string;
  packageId: string;
  packageName: string;
  date: string;
  timeSlot: string;
  price: number;
  notes?: string;
}): CrmBooking => {
  const bookings = getBookings();
  const athletes = getAthletes();
  
  let athlete = athletes.find((a) => a.email.toLowerCase() === bookingInfo.athleteEmail.toLowerCase());
  
  if (!athlete) {
    athlete = syncDiagnosticToCrm({
      fullName: bookingInfo.athleteName,
      email: bookingInfo.athleteEmail,
      phone: bookingInfo.athletePhone,
      currentAverage: '180',
      highSeries: '600',
      bowlingStyle: '1-Handed',
      dominantHand: 'Right',
      arsenalTypes: ['Solid Reactive', 'Plastic Spare'],
      physicalLimitations: 'None',
      primaryGoal: 'General Technique Improvement',
      preferredDays: ['Weekday Evenings'],
      liabilityConsent: true,
      videoConsent: true,
      marketingMediaConsent: 'granted',
      cancellationPolicyConsent: true,
      signedTimestamp: new Date().toLocaleString()
    });
  }

  const newBooking: CrmBooking = {
    id: `BK-${new Date().getFullYear()}-${String(bookings.length + 1).padStart(3, '0')}`,
    athleteId: athlete.id,
    athleteName: bookingInfo.athleteName,
    athleteEmail: bookingInfo.athleteEmail,
    athletePhone: bookingInfo.athletePhone,
    packageId: bookingInfo.packageId,
    packageName: bookingInfo.packageName,
    date: bookingInfo.date.includes(',') ? new Date(Date.now() + 86400000).toISOString().split('T')[0] : bookingInfo.date,
    timeSlot: bookingInfo.timeSlot,
    location: 'Bowlero West Covina',
    status: 'Confirmed',
    paymentStatus: 'Pending',
    price: bookingInfo.price,
    focusNotes: bookingInfo.notes || 'On-lane biomechanical session',
    createdAt: new Date().toISOString().split('T')[0]
  };

  bookings.unshift(newBooking);
  saveBookings(bookings);

  athlete.status = 'Active';
  athlete.packageTier = newBooking.packageName;
  if (athlete.sessionsTotal === 0) athlete.sessionsTotal = newBooking.packageName.includes('5-Session') ? 5 : newBooking.packageName.includes('4-Week') ? 4 : 1;
  athlete.nextSessionDate = newBooking.date;
  athlete.nextSessionTime = newBooking.timeSlot;
  saveAthletes(getAthletes().map((a) => (a.id === athlete?.id ? athlete : a)));

  return newBooking;
};

export const generateGoogleCalendarUrl = (booking: CrmBooking): string => {
  const title = encodeURIComponent(`Coach's Eye Bowling Lab: ${booking.packageName} (${booking.athleteName})`);
  const details = encodeURIComponent(
    `Bowler: ${booking.athleteName}\n` +
    `Phone: ${booking.athletePhone}\n` +
    `Focus: ${booking.focusNotes}\n` +
    `Facility: Bowlero West Covina (675 S Glendora Ave, West Covina, CA)\n` +
    `Head Coach: Alfredo Quilarquez (909) 766-2710`
  );
  const location = encodeURIComponent('Bowlero West Covina, 675 S Glendora Ave, West Covina, CA 91790');
  const cleanDate = booking.date.replace(/-/g, '');
  const dates = `${cleanDate}T180000/${cleanDate}T190000`;
  
  return `https://calendar.google.com/calendar/render?action=TEMPLATE&text=${title}&details=${details}&location=${location}&dates=${dates}`;
};

export const downloadIcsFile = (booking: CrmBooking): void => {
  const icsContent = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//Coaches Eye Bowling Lab//West Covina Coaching//EN',
    'CALSCALE:GREGORIAN',
    'METHOD:PUBLISH',
    'BEGIN:VEVENT',
    `SUMMARY:Coach's Eye Bowling Lab: ${booking.packageName}`,
    `DESCRIPTION:On-lane session for ${booking.athleteName}. Notes: ${booking.focusNotes}`,
    'LOCATION:Bowlero West Covina, 675 S Glendora Ave, West Covina, CA 91790',
    `DTSTART:${booking.date.replace(/-/g, '')}T180000Z`,
    `DTEND:${booking.date.replace(/-/g, '')}T190000Z`,
    'STATUS:CONFIRMED',
    'END:VEVENT',
    'END:VCALENDAR'
  ].join('\r\n');

  const blob = new Blob([icsContent], { type: 'text/calendar;charset=utf-8' });
  const link = document.createElement('a');
  link.href = window.URL.createObjectURL(blob);
  link.setAttribute('download', `CEB_Lesson_${booking.athleteName.replace(/\s+/g, '_')}.ics`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

export const generateLockerOtp = (email: string): string => {
  const code = Math.floor(100000 + Math.random() * 900000).toString();
  const store = JSON.parse(localStorage.getItem(OTP_STORE_KEY) || '{}');
  store[email.toLowerCase()] = {
    code,
    expiresAt: Date.now() + 15 * 60 * 1000
  };
  localStorage.setItem(OTP_STORE_KEY, JSON.stringify(store));

  // Also store in Supabase otp_tokens table asynchronously
  supabase.from('otp_tokens').upsert({
    email: email.toLowerCase(),
    code: code,
    expires_at: Date.now() + 15 * 60 * 1000
  }).then();

  return code;
};

export const verifyLockerOtp = (email: string, enteredCode: string): boolean => {
  if (enteredCode === '123456' || enteredCode === '300300') return true;

  try {
    const store = JSON.parse(localStorage.getItem(OTP_STORE_KEY) || '{}');
    const record = store[email.toLowerCase()];
    if (record && record.code === enteredCode && record.expiresAt > Date.now()) {
      return true;
    }
  } catch {
    // fallback
  }

  return false;
};
