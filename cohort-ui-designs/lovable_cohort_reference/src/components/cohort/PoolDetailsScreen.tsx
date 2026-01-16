import { motion } from "framer-motion";
import { ChevronLeft, Check, Sparkles } from "lucide-react";
import { cn } from "@/lib/utils";

interface PoolDetailsScreenProps {
  onBack: () => void;
}

type CircleStep = "forming" | "connecting" | "revealed";

export const PoolDetailsScreen = ({ onBack }: PoolDetailsScreenProps) => {
  const currentStep: CircleStep = "connecting";
  const circleId = "KCH-2026-01";
  const userStatus = "In Circle";

  const steps = [
    { id: "forming" as const, label: "Forming", description: "Circle is accepting members" },
    { id: "connecting" as const, label: "Connecting", description: "Finding meaningful connections" },
    { id: "revealed" as const, label: "Revealed", description: "Connections available" },
  ];

  const getStepState = (stepId: CircleStep) => {
    const stepOrder = ["forming", "connecting", "revealed"];
    const currentIndex = stepOrder.indexOf(currentStep);
    const stepIndex = stepOrder.indexOf(stepId);

    if (stepIndex < currentIndex) return "completed";
    if (stepIndex === currentIndex) return "active";
    return "pending";
  };

  return (
    <div className="screen-wrapper">
      {/* Header */}
      <motion.div 
        initial={{ opacity: 0, x: -10 }}
        animate={{ opacity: 1, x: 0 }}
        className="flex items-center gap-4 mb-10"
      >
        <motion.button
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
          onClick={onBack}
          className="w-10 h-10 rounded-full bg-secondary/50 flex items-center justify-center hover:bg-secondary transition-colors"
        >
          <ChevronLeft className="w-5 h-5 text-foreground" />
        </motion.button>
        <div>
          <h1 className="text-xl font-display text-foreground">January Circle</h1>
          <p className="text-xs text-muted-foreground">Kochi · 2026</p>
        </div>
      </motion.div>

      {/* Timeline - No card wrapper */}
      <motion.div 
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="mb-10"
      >
        <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-6">Timeline</h3>
        
        <div className="space-y-0">
          {steps.map((step, index) => {
            const state = getStepState(step.id);
            const isLast = index === steps.length - 1;

            return (
              <motion.div 
                key={step.id} 
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.15 + index * 0.1 }}
                className="relative"
              >
                <div className="flex items-start gap-4">
                  {/* Dot */}
                  <div className="relative">
                    <motion.div
                      initial={{ scale: 0.8 }}
                      animate={{ scale: 1 }}
                      className={cn(
                        "w-5 h-5 rounded-full border-2 flex items-center justify-center transition-all",
                        state === "completed" && "bg-primary border-primary",
                        state === "active" && "border-primary bg-background",
                        state === "pending" && "border-muted-foreground/30 bg-background"
                      )}
                    >
                      {state === "completed" && (
                        <Check className="w-3 h-3 text-primary-foreground" strokeWidth={3} />
                      )}
                      {state === "active" && (
                        <motion.div 
                          animate={{ scale: [1, 1.2, 1] }}
                          transition={{ repeat: Infinity, duration: 2 }}
                          className="w-2 h-2 rounded-full bg-primary" 
                        />
                      )}
                    </motion.div>
                    {/* Line */}
                    {!isLast && (
                      <div
                        className={cn(
                          "absolute top-5 left-1/2 -translate-x-1/2 w-0.5 h-12",
                          state === "completed" ? "bg-primary" : "bg-border"
                        )}
                      />
                    )}
                  </div>

                  {/* Content */}
                  <div className="pb-10">
                    <p
                      className={cn(
                        "text-sm font-medium",
                        state === "pending" ? "text-muted-foreground" : "text-foreground"
                      )}
                    >
                      {step.label}
                    </p>
                    <p className="text-xs text-muted-foreground mt-1">
                      {step.description}
                    </p>
                  </div>
                </div>
              </motion.div>
            );
          })}
        </div>
      </motion.div>

      {/* Divider */}
      <div className="editorial-divider flex justify-center mb-8">
        <span>Details</span>
      </div>

      {/* Details - Simple rows, no card */}
      <motion.div 
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.3 }}
        className="space-y-5"
      >
        <div className="flex justify-between items-center py-3 border-b border-border/30">
          <span className="text-sm text-muted-foreground">Circle ID</span>
          <span className="text-sm text-foreground font-mono">{circleId}</span>
        </div>
        <div className="flex justify-between items-center py-3 border-b border-border/30">
          <span className="text-sm text-muted-foreground">Location</span>
          <span className="text-sm text-foreground">Kochi</span>
        </div>
        <div className="flex justify-between items-center py-3">
          <span className="text-sm text-muted-foreground">Your status</span>
          <span className="inline-flex items-center gap-1.5 text-sm text-primary font-medium">
            <Sparkles className="w-3 h-3" />
            {userStatus}
          </span>
        </div>
      </motion.div>
    </div>
  );
};
