import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Button } from "@/components/ui/button";
import { Sparkles, Heart, MessageCircle, Shield, ChevronRight, ChevronLeft } from "lucide-react";
import cohortLogo from "@/assets/cohort-logo.png";

interface OnboardingTutorialProps {
  onComplete: () => void;
}

const TUTORIAL_STEPS = [
  {
    icon: Sparkles,
    title: "Enter the Circle",
    description: "Each month, a new Circle forms in your city. Join when you're ready to meet someone meaningful.",
    highlight: "Balanced & intentional",
  },
  {
    icon: Heart,
    title: "Connections Revealed",
    description: "When the Circle closes, we reveal connections based on genuine compatibility—not algorithms chasing engagement.",
    highlight: "Quality over quantity",
  },
  {
    icon: MessageCircle,
    title: "Meaningful Conversations",
    description: "Start a conversation with your connections. Take your time—there's no rush here.",
    highlight: "No pressure, no games",
  },
  {
    icon: Shield,
    title: "A Safe Space",
    description: "Every profile is verified. Report anything that doesn't feel right, and we'll handle it.",
    highlight: "Your safety matters",
  },
];

export const OnboardingTutorial = ({ onComplete }: OnboardingTutorialProps) => {
  const [currentStep, setCurrentStep] = useState(0);

  const handleNext = () => {
    if (currentStep < TUTORIAL_STEPS.length - 1) {
      setCurrentStep(currentStep + 1);
    } else {
      onComplete();
    }
  };

  const handlePrev = () => {
    if (currentStep > 0) {
      setCurrentStep(currentStep - 1);
    }
  };

  const handleSkip = () => {
    onComplete();
  };

  const step = TUTORIAL_STEPS[currentStep];
  const Icon = step.icon;
  const isLastStep = currentStep === TUTORIAL_STEPS.length - 1;

  return (
    <div className="screen-wrapper items-center justify-center">
      {/* Skip button */}
      <button
        onClick={handleSkip}
        className="absolute top-6 right-6 text-sm text-muted-foreground hover:text-foreground transition-colors"
      >
        Skip
      </button>

      {/* Logo */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="mb-10"
      >
        <img src={cohortLogo} alt="Cohort" className="w-14 h-14 mx-auto" />
      </motion.div>

      {/* Step content */}
      <AnimatePresence mode="wait">
        <motion.div
          key={currentStep}
          initial={{ opacity: 0, x: 50 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: -50 }}
          transition={{ duration: 0.3 }}
          className="flex flex-col items-center text-center px-6"
        >
          {/* Icon - minimal circle */}
          <motion.div
            initial={{ scale: 0.8 }}
            animate={{ scale: 1 }}
            transition={{ delay: 0.1, type: "spring", stiffness: 200 }}
            className="w-20 h-20 rounded-full bg-primary/8 flex items-center justify-center mb-10"
          >
            <Icon className="w-10 h-10 text-primary" />
          </motion.div>

          {/* Title */}
          <h2 className="font-display text-2xl text-foreground mb-4">
            {step.title}
          </h2>

          {/* Description */}
          <p className="text-muted-foreground leading-relaxed mb-6 max-w-[280px]">
            {step.description}
          </p>

          {/* Highlight - subtle text, no badge */}
          <p className="text-xs font-medium text-primary uppercase tracking-wider">
            {step.highlight}
          </p>
        </motion.div>
      </AnimatePresence>

      {/* Progress dots */}
      <div className="flex items-center gap-2 mt-14">
        {TUTORIAL_STEPS.map((_, index) => (
          <motion.div
            key={index}
            className={`h-1.5 rounded-full transition-all duration-300 ${
              index === currentStep
                ? "w-6 bg-primary"
                : index < currentStep
                ? "w-1.5 bg-primary/50"
                : "w-1.5 bg-muted"
            }`}
            initial={false}
            animate={{ scale: index === currentStep ? 1 : 1 }}
          />
        ))}
      </div>

      {/* Navigation */}
      <div className="flex items-center gap-4 mt-12 w-full max-w-xs">
        {currentStep > 0 && (
          <motion.div whileTap={{ scale: 0.95 }}>
            <Button
              variant="outline"
              size="icon"
              onClick={handlePrev}
              className="rounded-full w-12 h-12 border-border/50"
            >
              <ChevronLeft className="w-5 h-5" />
            </Button>
          </motion.div>
        )}
        
        <motion.div whileTap={{ scale: 0.98 }} className="flex-1">
          <Button
            variant="cohort"
            size="cohort"
            onClick={handleNext}
            className="w-full"
          >
            {isLastStep ? "Enter Cohort" : "Continue"}
            {!isLastStep && <ChevronRight className="w-5 h-5 ml-2" />}
          </Button>
        </motion.div>
      </div>
    </div>
  );
};
