import { motion, AnimatePresence } from "framer-motion";
import { ReactNode } from "react";

interface PageTransitionProps {
  children: ReactNode;
  screenKey: string;
  direction?: "left" | "right" | "up" | "down";
}

const getInitial = (direction: string) => ({
  opacity: 0,
  x: direction === "left" ? 30 : direction === "right" ? -30 : 0,
  y: direction === "up" ? 30 : direction === "down" ? -30 : 0,
  scale: 0.98,
});

const getExit = (direction: string) => ({
  opacity: 0,
  x: direction === "left" ? -30 : direction === "right" ? 30 : 0,
  y: direction === "up" ? -30 : direction === "down" ? 30 : 0,
  scale: 0.98,
});

export const PageTransition = ({ 
  children, 
  screenKey, 
  direction = "left" 
}: PageTransitionProps) => {
  return (
    <AnimatePresence mode="wait">
      <motion.div
        key={screenKey}
        initial={getInitial(direction)}
        animate={{ 
          opacity: 1, 
          x: 0, 
          y: 0, 
          scale: 1 
        }}
        exit={getExit(direction)}
        transition={{ 
          duration: 0.3, 
          ease: "easeOut" 
        }}
        className="w-full h-full"
      >
        {children}
      </motion.div>
    </AnimatePresence>
  );
};
