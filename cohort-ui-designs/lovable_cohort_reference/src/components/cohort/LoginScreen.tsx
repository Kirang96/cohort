import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Loader2 } from "lucide-react";
import cohortLogo from "@/assets/cohort-logo.png";

interface LoginScreenProps {
  onLogin: (isNewUser: boolean) => void;
}

const GoogleIcon = () => (
  <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M17.64 9.20443C17.64 8.56625 17.5827 7.95262 17.4764 7.36353H9V10.8449H13.8436C13.635 11.9699 13.0009 12.9231 12.0477 13.5613V15.8194H14.9564C16.6582 14.2526 17.64 11.9453 17.64 9.20443Z" fill="#4285F4"/>
    <path d="M9 18C11.43 18 13.4673 17.1941 14.9564 15.8195L12.0477 13.5613C11.2418 14.1013 10.2109 14.4204 9 14.4204C6.65591 14.4204 4.67182 12.8372 3.96409 10.71H0.957275V13.0418C2.43818 15.9831 5.48182 18 9 18Z" fill="#34A853"/>
    <path d="M3.96409 10.71C3.78409 10.17 3.68182 9.59314 3.68182 9.00001C3.68182 8.40687 3.78409 7.83001 3.96409 7.29001V4.95819H0.957273C0.347727 6.17319 0 7.54773 0 9.00001C0 10.4523 0.347727 11.8268 0.957273 13.0418L3.96409 10.71Z" fill="#FBBC05"/>
    <path d="M9 3.57955C10.3214 3.57955 11.5077 4.03364 12.4405 4.92545L15.0218 2.34409C13.4632 0.891818 11.4259 0 9 0C5.48182 0 2.43818 2.01682 0.957275 4.95818L3.96409 7.29C4.67182 5.16273 6.65591 3.57955 9 3.57955Z" fill="#EA4335"/>
  </svg>
);

export const LoginScreen = ({ onLogin }: LoginScreenProps) => {
  const [phone, setPhone] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [showOtp, setShowOtp] = useState(false);
  const [otp, setOtp] = useState("");
  const [isGoogleLoading, setIsGoogleLoading] = useState(false);
  const [isExistingUser, setIsExistingUser] = useState(false);

  const handleContinue = () => {
    if (phone.length >= 10) {
      setIsLoading(true);
      setTimeout(() => {
        setIsLoading(false);
        setShowOtp(true);
      }, 1500);
    }
  };

  const handleVerify = () => {
    if (otp.length === 6) {
      setIsLoading(true);
      setTimeout(() => {
        setIsLoading(false);
        onLogin(!isExistingUser);
      }, 1500);
    }
  };

  const handleGoogleSignIn = () => {
    setIsGoogleLoading(true);
    // Simulate Google sign-in
    setTimeout(() => {
      setIsGoogleLoading(false);
      onLogin(!isExistingUser);
    }, 2000);
  };

  return (
    <div className="screen-wrapper justify-center">
      <div className="flex-1 flex flex-col justify-center max-w-sm mx-auto w-full">
        {/* Logo & Tagline */}
        <div className="text-center mb-12 animate-fade-in">
          <img 
            src={cohortLogo} 
            alt="Cohort" 
            className="w-20 h-20 mx-auto mb-4"
          />
          <h1 className="text-4xl font-display text-foreground mb-3 tracking-tight">
            Cohort
          </h1>
          <p className="text-muted-foreground text-sm tracking-wide">
            Balanced connections, by design.
          </p>
        </div>

        {/* Login/Signup Toggle */}
        <div className="flex items-center justify-center gap-2 mb-6 animate-fade-in">
          <button
            onClick={() => setIsExistingUser(false)}
            className={`px-4 py-2 text-sm font-medium rounded-full transition-all duration-200 ${
              !isExistingUser 
                ? "bg-primary text-primary-foreground" 
                : "text-muted-foreground hover:text-foreground"
            }`}
          >
            Sign up
          </button>
          <button
            onClick={() => setIsExistingUser(true)}
            className={`px-4 py-2 text-sm font-medium rounded-full transition-all duration-200 ${
              isExistingUser 
                ? "bg-primary text-primary-foreground" 
                : "text-muted-foreground hover:text-foreground"
            }`}
          >
            Log in
          </button>
        </div>

        {/* Input Section */}
        <div className="space-y-5 animate-fade-in stagger-1">
          {!showOtp ? (
            <>
              {/* Google Sign In */}
              <button
                onClick={handleGoogleSignIn}
                disabled={isGoogleLoading}
                className="google-button"
              >
                {isGoogleLoading ? (
                  <Loader2 className="w-5 h-5 animate-spin" />
                ) : (
                  <>
                    <GoogleIcon />
                    <span>{isExistingUser ? "Log in" : "Sign up"} with Google</span>
                  </>
                )}
              </button>

              {/* Divider */}
              <div className="editorial-divider flex items-center justify-center">
                <span>or use phone</span>
              </div>

              <div className="space-y-2">
                <label className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                  Phone number
                </label>
                <div className="flex gap-3">
                  <div className="flex items-center px-4 bg-secondary rounded-xl border border-border text-sm text-muted-foreground font-medium">
                    +91
                  </div>
                  <Input
                    type="tel"
                    placeholder="Enter your number"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value.replace(/\D/g, "").slice(0, 10))}
                    className="flex-1 h-12 rounded-xl border-border bg-card text-foreground placeholder:text-muted-foreground/60"
                  />
                </div>
              </div>

              <Button
                variant="cohort"
                size="cohort"
                onClick={handleContinue}
                disabled={phone.length < 10 || isLoading}
              >
                {isLoading ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Sending OTP…</span>
                  </>
                ) : (
                  isExistingUser ? "Log in" : "Continue"
                )}
              </Button>
            </>
          ) : (
            <>
              <div className="space-y-2">
                <label className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                  Verification code
                </label>
                <p className="text-sm text-muted-foreground">
                  Sent to +91 {phone}
                </p>
                <Input
                  type="text"
                  placeholder="000000"
                  value={otp}
                  onChange={(e) => setOtp(e.target.value.replace(/\D/g, "").slice(0, 6))}
                  className="h-14 rounded-xl border-border bg-card text-foreground text-center text-xl font-semibold tracking-[0.4em] placeholder:tracking-[0.4em] placeholder:text-muted-foreground/40"
                  maxLength={6}
                />
              </div>

              <Button
                variant="cohort"
                size="cohort"
                onClick={handleVerify}
                disabled={otp.length < 6 || isLoading}
              >
                {isLoading ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Verifying…</span>
                  </>
                ) : (
                  "Verify & Continue"
                )}
              </Button>

              <button
                onClick={() => setShowOtp(false)}
                className="text-sm text-muted-foreground hover:text-foreground transition-smooth w-full text-center py-2"
              >
                Change number
              </button>
            </>
          )}
        </div>

        {/* Footer */}
        <p className="text-2xs text-muted-foreground text-center mt-12 px-8 leading-relaxed animate-fade-in stagger-3">
          By continuing, you agree to our Terms of Service and Privacy Policy
        </p>
      </div>
    </div>
  );
};
