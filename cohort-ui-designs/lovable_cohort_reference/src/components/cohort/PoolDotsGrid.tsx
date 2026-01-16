import { cn } from "@/lib/utils";
import { motion } from "framer-motion";

interface PoolDotsGridProps {
  maleCount: number;
  femaleCount: number;
  maxCount: number;
  className?: string;
}

export const PoolDotsGrid = ({ 
  maleCount, 
  femaleCount, 
  maxCount, 
  className 
}: PoolDotsGridProps) => {
  // Create an array representing all dots (50 total: 25 male slots + 25 female slots)
  const maleDots = Array.from({ length: maxCount }, (_, i) => ({
    type: i < maleCount ? "male" : "empty",
    index: i,
  }));

  const femaleDots = Array.from({ length: maxCount }, (_, i) => ({
    type: i < femaleCount ? "female" : "empty",
    index: i,
  }));

  const containerVariants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: {
        staggerChildren: 0.02,
      },
    },
  };

  const dotVariants = {
    hidden: { scale: 0, opacity: 0 },
    visible: { 
      scale: 1, 
      opacity: 1,
      transition: {
        type: "spring" as const,
        stiffness: 300,
        damping: 20,
      },
    },
  };

  return (
    <div className={cn("space-y-4", className)}>
      {/* Male section */}
      <div className="space-y-2">
        <div className="flex items-center justify-between text-xs">
          <span className="font-medium text-muted-foreground uppercase tracking-wider">Male</span>
          <span className="font-semibold text-foreground">{maleCount}/{maxCount}</span>
        </div>
        <motion.div 
          className="flex flex-wrap gap-1.5"
          variants={containerVariants}
          initial="hidden"
          animate="visible"
        >
          {maleDots.map((dot) => (
            <motion.div
              key={`male-${dot.index}`}
              variants={dotVariants}
              className={cn(
                "w-3 h-3 rounded-full transition-colors duration-300",
                dot.type === "male" ? "dot-male" : "dot-empty"
              )}
            />
          ))}
        </motion.div>
      </div>

      {/* Female section */}
      <div className="space-y-2">
        <div className="flex items-center justify-between text-xs">
          <span className="font-medium text-muted-foreground uppercase tracking-wider">Female</span>
          <span className="font-semibold text-foreground">{femaleCount}/{maxCount}</span>
        </div>
        <motion.div 
          className="flex flex-wrap gap-1.5"
          variants={containerVariants}
          initial="hidden"
          animate="visible"
        >
          {femaleDots.map((dot) => (
            <motion.div
              key={`female-${dot.index}`}
              variants={dotVariants}
              className={cn(
                "w-3 h-3 rounded-full transition-colors duration-300",
                dot.type === "female" ? "dot-female" : "dot-empty"
              )}
            />
          ))}
        </motion.div>
      </div>
    </div>
  );
};
