import { motion } from "framer-motion";
import { cn } from "@/lib/utils";
import { MessageCircle, Sparkles } from "lucide-react";

type ChatStatus = "active" | "continued" | "expired";

interface Chat {
  id: string;
  name: string;
  lastMessage: string;
  status: ChatStatus;
}

interface ChatListScreenProps {
  onOpenChat: (chatId: string) => void;
}

export const ChatListScreen = ({ onOpenChat }: ChatListScreenProps) => {
  // Mock chats data
  const chats: Chat[] = [
    { id: "1", name: "Priya", lastMessage: "That sounds wonderful! Would love to...", status: "active" },
    { id: "2", name: "Sneha", lastMessage: "Thanks for the conversation!", status: "continued" },
    { id: "3", name: "Anjali", lastMessage: "It was nice talking to you", status: "expired" },
  ];

  const groupedChats = {
    active: chats.filter((c) => c.status === "active"),
    continued: chats.filter((c) => c.status === "continued"),
    expired: chats.filter((c) => c.status === "expired"),
  };

  const statusLabels: Record<ChatStatus, string> = {
    active: "Active",
    continued: "Continued",
    expired: "Ended",
  };

  const renderSection = (status: ChatStatus, chatList: Chat[]) => {
    if (chatList.length === 0) return null;

    return (
      <motion.div 
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        className="mb-6"
      >
        <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-3">
          {statusLabels[status]}
        </h3>
        <div className="space-y-1">
          {chatList.map((chat, index) => (
            <motion.button
              key={chat.id}
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: index * 0.05 }}
              whileHover={{ x: 4 }}
              whileTap={{ scale: 0.99 }}
              onClick={() => onOpenChat(chat.id)}
              className={cn(
                "w-full flex items-center gap-4 py-4 text-left",
                "border-b border-border/30 last:border-0",
                "hover:bg-secondary/30 -mx-2 px-2 rounded-lg transition-colors",
                status === "expired" && "opacity-60"
              )}
            >
              {/* Avatar */}
              <div className={cn(
                "w-12 h-12 rounded-full flex items-center justify-center shrink-0",
                status === "active" ? "bg-gradient-to-br from-primary/20 to-primary/5" : "bg-secondary/50"
              )}>
                <span className={cn(
                  "text-lg font-display",
                  status === "active" ? "text-primary" : "text-muted-foreground"
                )}>
                  {chat.name[0]}
                </span>
              </div>

              {/* Content */}
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between mb-1">
                  <span className="font-medium text-foreground">{chat.name}</span>
                  <span
                    className={cn(
                      "text-xs px-2 py-0.5 rounded-full",
                      status === "active" && "bg-primary/10 text-primary",
                      status === "continued" && "bg-secondary text-muted-foreground",
                      status === "expired" && "bg-muted/50 text-muted-foreground"
                    )}
                  >
                    {statusLabels[status]}
                  </span>
                </div>
                <p className="text-sm text-muted-foreground truncate">
                  {chat.lastMessage}
                </p>
              </div>
            </motion.button>
          ))}
        </div>
      </motion.div>
    );
  };

  return (
    <div className="screen-wrapper pb-24">
      {/* Header */}
      <motion.div 
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className="mb-8"
      >
        <h1 className="text-2xl font-display text-foreground">Conversations</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Your ongoing connections
        </p>
      </motion.div>

      {/* Chat Lists */}
      <div>
        {renderSection("active", groupedChats.active)}
        {renderSection("continued", groupedChats.continued)}
        {renderSection("expired", groupedChats.expired)}

        {chats.length === 0 && (
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="text-center py-16"
          >
            <Sparkles className="w-10 h-10 mx-auto mb-4 text-muted-foreground/40" />
            <p className="text-sm text-muted-foreground mb-2">No conversations yet</p>
            <p className="text-xs text-muted-foreground/70">
              Start chatting with your connections
            </p>
          </motion.div>
        )}
      </div>
    </div>
  );
};
