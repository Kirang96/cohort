import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { X, Shield, Flag } from "lucide-react";
import { cn } from "@/lib/utils";

interface SafetyModalProps {
  isOpen: boolean;
  onClose: () => void;
}

type ReportReason = "inappropriate" | "harassment" | "spam" | "fake" | "other";

export const SafetyModal = ({ isOpen, onClose }: SafetyModalProps) => {
  const [view, setView] = useState<"main" | "report">("main");
  const [selectedReason, setSelectedReason] = useState<ReportReason | null>(null);
  const [additionalInfo, setAdditionalInfo] = useState("");

  const reportReasons: { id: ReportReason; label: string }[] = [
    { id: "inappropriate", label: "Inappropriate content" },
    { id: "harassment", label: "Harassment or abuse" },
    { id: "spam", label: "Spam or scam" },
    { id: "fake", label: "Fake profile" },
    { id: "other", label: "Other" },
  ];

  const handleBlock = () => {
    // Handle block
    onClose();
  };

  const handleSubmitReport = () => {
    if (selectedReason) {
      // Handle report submission
      onClose();
      setView("main");
      setSelectedReason(null);
      setAdditionalInfo("");
    }
  };

  const handleClose = () => {
    onClose();
    setView("main");
    setSelectedReason(null);
    setAdditionalInfo("");
  };

  if (!isOpen) return null;

  return (
    <>
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-foreground/20 z-40"
        onClick={handleClose}
      />

      {/* Modal */}
      <div className="absolute bottom-0 left-0 right-0 bg-background rounded-t-2xl p-6 z-50 animate-slide-up">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-semibold text-foreground">
            {view === "main" ? "Safety Actions" : "Report User"}
          </h2>
          <button
            onClick={handleClose}
            className="w-8 h-8 rounded-lg flex items-center justify-center hover:bg-secondary transition-smooth"
          >
            <X className="w-5 h-5 text-muted-foreground" />
          </button>
        </div>

        {view === "main" ? (
          <div className="space-y-3">
            <button
              onClick={handleBlock}
              className="w-full flex items-center gap-4 p-4 rounded-xl border border-border hover:bg-secondary/50 transition-smooth text-left"
            >
              <div className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center">
                <Shield className="w-5 h-5 text-muted-foreground" />
              </div>
              <div>
                <p className="font-medium text-foreground">Block user</p>
                <p className="text-sm text-muted-foreground">
                  They won't be able to contact you
                </p>
              </div>
            </button>

            <button
              onClick={() => setView("report")}
              className="w-full flex items-center gap-4 p-4 rounded-xl border border-border hover:bg-secondary/50 transition-smooth text-left"
            >
              <div className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center">
                <Flag className="w-5 h-5 text-muted-foreground" />
              </div>
              <div>
                <p className="font-medium text-foreground">Report user</p>
                <p className="text-sm text-muted-foreground">
                  Let us know what happened
                </p>
              </div>
            </button>
          </div>
        ) : (
          <div className="space-y-4">
            <p className="text-sm text-muted-foreground">
              Select the reason for reporting this user.
            </p>

            <div className="space-y-2">
              {reportReasons.map((reason) => (
                <button
                  key={reason.id}
                  onClick={() => setSelectedReason(reason.id)}
                  className={cn(
                    "w-full p-4 rounded-xl border text-left transition-smooth",
                    selectedReason === reason.id
                      ? "border-primary bg-accent"
                      : "border-border hover:bg-secondary/50"
                  )}
                >
                  <span className="text-sm font-medium text-foreground">
                    {reason.label}
                  </span>
                </button>
              ))}
            </div>

            <div className="space-y-2">
              <label className="text-sm text-muted-foreground">
                Additional details (optional)
              </label>
              <Textarea
                value={additionalInfo}
                onChange={(e) => setAdditionalInfo(e.target.value)}
                placeholder="Tell us more about what happened..."
                className="resize-none rounded-xl border-border bg-background min-h-[80px]"
              />
            </div>

            <div className="flex gap-3 pt-2">
              <Button
                variant="cohort-secondary"
                size="cohort"
                onClick={() => setView("main")}
                className="flex-1"
              >
                Back
              </Button>
              <Button
                variant="cohort"
                size="cohort"
                onClick={handleSubmitReport}
                disabled={!selectedReason}
                className="flex-1"
              >
                Submit Report
              </Button>
            </div>
          </div>
        )}
      </div>
    </>
  );
};
