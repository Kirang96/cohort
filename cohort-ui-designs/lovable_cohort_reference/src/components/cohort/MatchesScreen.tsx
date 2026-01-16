import { motion } from "framer-motion";
import { Button } from "@/components/ui/button";
import { Heart, MessageCircle, Sparkles } from "lucide-react";

interface Connection {
  id: string;
  name: string;
  age: number;
  sharedInterests: string[];
  compatibilityScore: number;
}

interface MatchesScreenProps {
  onOpenChat: (matchId: string) => void;
}

export const MatchesScreen = ({ onOpenChat }: MatchesScreenProps) => {
  // Mock connections data
  const connections: Connection[] = [
    {
      id: "1",
      name: "Priya",
      age: 26,
      sharedInterests: ["Reading", "Travel", "Coffee"],
      compatibilityScore: 85,
    },
    {
      id: "2",
      name: "Sneha",
      age: 24,
      sharedInterests: ["Music", "Photography"],
      compatibilityScore: 72,
    },
    {
      id: "3",
      name: "Anjali",
      age: 27,
      sharedInterests: ["Art", "Movies", "Nature"],
      compatibilityScore: 78,
    },
  ];

  return (
    <div className="screen-wrapper pb-24">
      {/* Header */}
      <motion.div 
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className="mb-8"
      >
        <h1 className="text-2xl font-display text-foreground">Connections</h1>
        <p className="text-sm text-muted-foreground mt-2 max-w-[280px]">
          People who resonated with your energy. Take your time.
        </p>
      </motion.div>

      {/* Connections List */}
      <div className="space-y-1">
        {connections.length > 0 ? (
          connections.map((connection, index) => (
            <motion.div
              key={connection.id}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.08 }}
              whileHover={{ x: 4 }}
              className="py-5 border-b border-border/30 last:border-0"
            >
              <div className="flex items-start gap-4">
                {/* Avatar */}
                <div className="w-14 h-14 rounded-full bg-gradient-to-br from-primary/20 to-primary/5 flex items-center justify-center shrink-0">
                  <span className="text-xl font-display text-primary">
                    {connection.name[0]}
                  </span>
                </div>

                {/* Info */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between mb-1">
                    <h3 className="text-base font-medium text-foreground">
                      {connection.name}
                    </h3>
                    <div className="flex items-center gap-1 text-xs text-primary">
                      <Heart className="w-3 h-3" />
                      <span>{connection.compatibilityScore}%</span>
                    </div>
                  </div>
                  <p className="text-xs text-muted-foreground mb-3">{connection.age} years</p>

                  {/* Shared Interests */}
                  <div className="flex flex-wrap gap-1.5 mb-4">
                    {connection.sharedInterests.slice(0, 3).map((interest) => (
                      <span key={interest} className="text-xs px-2 py-1 rounded-full bg-secondary/50 text-muted-foreground">
                        {interest}
                      </span>
                    ))}
                    {connection.sharedInterests.length > 3 && (
                      <span className="text-xs px-2 py-1 text-muted-foreground">
                        +{connection.sharedInterests.length - 3}
                      </span>
                    )}
                  </div>

                  {/* Action */}
                  <motion.div whileTap={{ scale: 0.98 }}>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => onOpenChat(connection.id)}
                      className="h-9 px-4 rounded-full border-border/50 hover:bg-secondary/50"
                    >
                      <MessageCircle className="w-3.5 h-3.5 mr-2" />
                      Start conversation
                    </Button>
                  </motion.div>
                </div>
              </div>
            </motion.div>
          ))
        ) : (
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="text-center py-16"
          >
            <Sparkles className="w-10 h-10 mx-auto mb-4 text-muted-foreground/40" />
            <p className="text-sm text-muted-foreground mb-2">No connections yet</p>
            <p className="text-xs text-muted-foreground/70">Enter a Circle to meet someone meaningful</p>
          </motion.div>
        )}
      </div>
    </div>
  );
};
