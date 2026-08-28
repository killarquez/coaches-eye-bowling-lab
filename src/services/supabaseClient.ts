import { createClient } from '@supabase/supabase-js';

export const SUPABASE_URL = 'https://vadfqzddfjcphskpfbzv.supabase.co';
export const SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZhZGZxemRkZmpjcGhza3BmYnp2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODc4ODk2MDgsImV4cCI6MjEwMzQ2NTYwOH0.kC27jDRMQM-UFQsuM32EFYY-3X5h2Wj6NwVDRHiPNcQ';

export const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);
