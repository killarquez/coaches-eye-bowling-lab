export type BowlingStyle = '1-Handed' | '2-Handed';
export type DominantHand = 'Right' | 'Left';

export interface SessionVideo {
  id: string;
  title: string;
  url: string; // YouTube Unlisted, Google Drive, or MP4
  date: string;
  fps: number; // e.g. 240
  coachingNotes: string;
  keyCheckpoints: string[];
}

export interface DrillPrescription {
  id: string;
  title: string;
  category: 'Footwork & Balance' | 'Swing Plane' | 'Release & Leverage' | '3-6-9 Spares' | 'Lane Transition';
  instructions: string;
  targetReps: string;
  completed: boolean;
}

export interface BallSpecItem {
  name: string;
  coverstock: 'Solid Reactive' | 'Pearl Reactive' | 'Hybrid Reactive' | 'Urethane' | 'Plastic Spare';
  weight: string;
  layout?: string; // e.g. "50 x 4.5 x 35"
  notes?: string;
}

export interface AthleteProfile {
  id: string; // e.g. "CEB-101"
  fullName: string;
  email: string;
  phone: string;
  style: BowlingStyle;
  dominantHand: DominantHand;
  bookAverage: number;
  careerHighSeries: number;
  careerHighGame?: number;
  primaryGoal: string;
  physicalLimitations: string;
  
  // Biomechanical Specs
  papCoordinates?: string; // e.g. "4 1/2 over by 1/2 up"
  axisTiltDeg?: number;
  axisRotationDeg?: number;
  averageSpeedMph?: number;
  estimatedRevRateRpm?: number;

  // Package & Training History
  packageTier: string; // e.g. "5-Session Progressive Blueprint"
  sessionsTotal: number;
  sessionsCompleted: number;
  nextSessionDate?: string;
  nextSessionTime?: string;
  clinicCohort?: string;

  // Storage & Organization
  googleDriveFolderUrl: string;
  arsenal: BallSpecItem[];
  videos: SessionVideo[];
  drills: DrillPrescription[];

  // Legal & Consent
  signedWaiverTimestamp: string;
  marketingConsent: 'granted' | 'private_only';
  isMinor: boolean;
  parentGuardianName?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;

  createdAt: string;
  lastCoachedAt: string;
}

export type BookingStatus = 'Confirmed' | 'Completed' | 'Rescheduled' | 'Cancelled';
export type PaymentStatus = 'Paid' | 'Pending' | 'Refunded';

export interface CrmBooking {
  id: string; // e.g. "BK-2026-001"
  athleteId: string;
  athleteName: string;
  athleteEmail: string;
  athletePhone: string;
  packageId: string;
  packageName: string;
  date: string; // e.g. "2026-09-02"
  timeSlot: string; // e.g. "6:00 PM - 7:00 PM"
  location: string; // "Bowlero West Covina"
  laneAssignment?: string; // e.g. "Lanes 23-24"
  status: BookingStatus;
  paymentStatus: PaymentStatus;
  price: number;
  focusNotes: string;
  googleCalendarEventUrl?: string;
  createdAt: string;
}

export interface FinancialRecord {
  id: string;
  bookingId: string;
  athleteName: string;
  date: string;
  description: string;
  packageType: string;
  amount: number;
  status: PaymentStatus;
  method: 'Square / Stripe' | 'Cash / Zelle' | 'Pending';
}

export interface CrmMetricSummary {
  totalAthletes: number;
  monthlyRevenue: number;
  upcomingSessionsCount: number;
  averagePinImprovement: number;
  clinicCapacityPercent: number;
}
