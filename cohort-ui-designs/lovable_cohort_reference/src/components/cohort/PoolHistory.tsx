import { motion } from "framer-motion";
import { Calendar, Heart, Sparkles } from "lucide-react";
import { cn } from "@/lib/utils";

interface CircleHistoryItem {
  id: string;
  month: string;
  year: string;
  location: string;
  connectionsReceived: number;
  status: "completed" | "no-connection";
}

const MOCK_HISTORY: CircleHistoryItem[] = [
  {
    id: "1",
    month: "December",
    year: "2025",
    location: "Kochi",
    connectionsReceived: 4,
    status: "completed",
  },
  {
    id: "2",
    month: "November",
    year: "2025",
    location: "Kochi",
    connectionsReceived: 3,
    status: "completed",
  },
  {
    id: "3",
    month: "October",
    year: "2025",
    location: "Kochi",
    connectionsReceived: 0,
    status: "no-connection",
  },
];

export const PoolHistory = () => {
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between mb-2">
        <h3 className="text-sm font-semibold text-foreground uppercase tracking-wide">Past Circles</h3>
        <span className="text-xs text-muted-foreground">
          {MOCK_HISTORY.length} circles
        </span>
      </div>

      {MOCK_HISTORY.length === 0 ? (
        <div className="text-center py-10 text-muted-foreground text-sm">
          <Sparkles className="w-8 h-8 mx-auto mb-3 opacity-40" />
          <p>No circles yet. Enter your first one!</p>
        </div>
      ) : (
        <div className="space-y-1">
          {MOCK_HISTORY.map((circle, index) => (
            <motion.div
              key={circle.id}
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: index * 0.08 }}
              whileHover={{ x: 4 }}
              whileTap={{ scale: 0.99 }}
              className={cn(
                "flex items-center gap-4 py-4 cursor-pointer group",
                "border-b border-border/30 last:border-0",
                "transition-colors hover:bg-secondary/30 -mx-2 px-2 rounded-lg"
              )}
            >
              {/* Icon */}
              <div className={cn(
                "w-10 h-10 rounded-full flex items-center justify-center shrink-0",
                circle.status === "completed" 
                  ? "bg-primary/10" 
                  : "bg-muted/50"
              )}>
                {circle.status === "completed" ? (
                  <Heart className="w-4 h-4 text-primary" />
                ) : (
                  <Sparkles className="w-4 h-4 text-muted-foreground" />
                )}
              </div>

              {/* Details */}
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <span className="text-sm font-medium text-foreground">
                    {circle.month} Circle
                  </span>
                  <span className="text-xs text-muted-foreground">
                    {circle.year}
                  </span>
                </div>
                <p className="text-xs text-muted-foreground mt-0.5">
                  {circle.location}
                </p>
              </div>

              {/* Connection count */}
              <div className="text-right">
                {circle.status === "completed" ? (
                  <div className="flex items-center gap-1.5">
                    <span className="text-sm font-semibold text-primary">
                      {circle.connectionsReceived}
                    </span>
                    <span className="text-xs text-muted-foreground">
                      connections
                    </span>
                  </div>
                ) : (
                  <span className="text-xs text-muted-foreground">
                    No connections
                  </span>
                )}
              </div>
            </motion.div>
          ))}
        </div>
      )}
    </div>
  );
};
