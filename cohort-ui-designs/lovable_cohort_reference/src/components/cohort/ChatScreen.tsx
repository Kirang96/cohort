import { useState } from "react";
import { motion } from "framer-motion";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ChevronLeft, Image, Send, MoreVertical, Clock } from "lucide-react";
import { cn } from "@/lib/utils";
import { ContinueRequestModal } from "./ContinueRequestModal";

type ChatStatus = "active" | "expiring" | "continued" | "expired" | "request_sent" | "request_received";

interface Message {
  id: string;
  content: string;
  type: "text" | "image" | "system";
  isSent: boolean;
  timestamp: string;
}

interface ChatScreenProps {
  chatId: string;
  status?: ChatStatus;
  onBack: () => void;
  onOpenSafetyModal: () => void;
}

// Haptic feedback simulation
const hapticFeedback = {
  light: { scale: [1, 0.97, 1], transition: { duration: 0.1 } },
  medium: { scale: [1, 0.95, 1], transition: { duration: 0.15 } },
  heavy: { scale: [1, 0.92, 1], transition: { duration: 0.2 } },
};

export const ChatScreen = ({ chatId, status = "expiring", onBack, onOpenSafetyModal }: ChatScreenProps) => {
  const [message, setMessage] = useState("");
  const [chatStatus, setChatStatus] = useState<ChatStatus>(status);
  const [showContinueModal, setShowContinueModal] = useState(false);
  
  // Mock data
  const chatName = "Priya";
  const expiresIn = "2h";

  const messages: Message[] = [
    { id: "1", content: "Hi! Nice to connect with you", type: "text", isSent: false, timestamp: "10:30 AM" },
    { id: "2", content: "Hey! Same here. I noticed we both love traveling", type: "text", isSent: true, timestamp: "10:32 AM" },
    { id: "3", content: "Yes! I've been to most of South India. Kochi is relatively new for me though", type: "text", isSent: false, timestamp: "10:35 AM" },
    { id: "4", content: "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400", type: "image", isSent: false, timestamp: "10:36 AM" },
    { id: "5", content: "That looks amazing! Where is this?", type: "text", isSent: true, timestamp: "10:38 AM" },
  ];

  const handleSend = () => {
    if (message.trim()) {
      setMessage("");
    }
  };

  const handleSendContinueRequest = () => {
    setChatStatus("request_sent");
    setShowContinueModal(false);
  };

  const handleAcceptContinueRequest = () => {
    setChatStatus("continued");
    setShowContinueModal(false);
  };

  const getStatusText = () => {
    switch (chatStatus) {
      case "expired": return "Conversation ended";
      case "continued": return "Conversation continued ✓";
      case "request_sent": return "Continue request sent";
      case "request_received": return "Wants to continue";
      case "expiring": return `Expires in ${expiresIn}`;
      default: return "Active";
    }
  };

  const getStatusColor = () => {
    switch (chatStatus) {
      case "expiring": return "text-amber-600";
      case "continued": return "text-primary";
      case "request_received": return "text-primary";
      default: return "text-muted-foreground";
    }
  };

  return (
    <div className="screen-wrapper p-0 flex flex-col">
      {/* Header */}
      <motion.div 
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex items-center gap-3 px-4 py-3 border-b border-border/30 bg-background"
      >
        <motion.button
          whileHover={{ scale: 1.05 }}
          whileTap={hapticFeedback.light}
          onClick={onBack}
          className="w-10 h-10 rounded-full bg-secondary/50 flex items-center justify-center hover:bg-secondary transition-colors"
        >
          <ChevronLeft className="w-5 h-5 text-foreground" />
        </motion.button>
        
        <div className="flex items-center gap-3 flex-1">
          <div className="w-10 h-10 rounded-full bg-gradient-to-br from-primary/20 to-primary/5 flex items-center justify-center">
            <span className="text-base font-display text-primary">{chatName[0]}</span>
          </div>
          <div>
            <h2 className="font-medium text-foreground">{chatName}</h2>
            <p className={cn("text-xs flex items-center gap-1", getStatusColor())}>
              {chatStatus === "expiring" && <Clock className="w-3 h-3" />}
              {getStatusText()}
            </p>
          </div>
        </div>

        <motion.button
          whileHover={{ scale: 1.05 }}
          whileTap={hapticFeedback.light}
          onClick={onOpenSafetyModal}
          className="w-10 h-10 rounded-full flex items-center justify-center hover:bg-secondary/50 transition-colors"
        >
          <MoreVertical className="w-5 h-5 text-muted-foreground" />
        </motion.button>
      </motion.div>

      {/* Expiring Banner */}
      {chatStatus === "expiring" && (
        <motion.div
          initial={{ opacity: 0, height: 0 }}
          animate={{ opacity: 1, height: "auto" }}
          className="px-4 py-3 bg-amber-50 dark:bg-amber-950/30 border-b border-amber-200/50 dark:border-amber-800/30"
        >
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Clock className="w-4 h-4 text-amber-600" />
              <span className="text-sm text-amber-800 dark:text-amber-200">
                Conversation expiring soon
              </span>
            </div>
            <motion.button
              whileTap={hapticFeedback.medium}
              onClick={() => setShowContinueModal(true)}
              className="text-sm font-medium text-primary hover:underline"
            >
              Request more time
            </motion.button>
          </div>
        </motion.div>
      )}

      {/* Request Received Banner */}
      {chatStatus === "request_received" && (
        <motion.div
          initial={{ opacity: 0, height: 0 }}
          animate={{ opacity: 1, height: "auto" }}
          className="px-4 py-3 bg-primary/5 border-b border-primary/20"
        >
          <div className="flex items-center justify-between">
            <span className="text-sm text-foreground">
              {chatName} wants to continue
            </span>
            <motion.button
              whileTap={hapticFeedback.medium}
              onClick={() => setShowContinueModal(true)}
              className="text-sm font-medium text-primary hover:underline"
            >
              Respond
            </motion.button>
          </div>
        </motion.div>
      )}

      {/* Request Sent Banner */}
      {chatStatus === "request_sent" && (
        <motion.div
          initial={{ opacity: 0, height: 0 }}
          animate={{ opacity: 1, height: "auto" }}
          className="px-4 py-3 bg-secondary/50 border-b border-border/30"
        >
          <p className="text-sm text-muted-foreground text-center">
            Continue request sent. Waiting for {chatName}'s response...
          </p>
        </motion.div>
      )}

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-3">
        {messages.map((msg, index) => (
          <motion.div
            key={msg.id}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: index * 0.05 }}
            className={cn("flex", msg.isSent ? "justify-end" : "justify-start")}
          >
            {msg.type === "text" ? (
              <motion.div
                whileTap={hapticFeedback.light}
                className={cn(
                  "chat-bubble cursor-default",
                  msg.isSent ? "chat-bubble-sent" : "chat-bubble-received"
                )}
              >
                {msg.content}
              </motion.div>
            ) : (
              <motion.div
                whileTap={hapticFeedback.light}
                className={cn(
                  "max-w-[70%] rounded-2xl overflow-hidden cursor-default",
                  msg.isSent ? "ml-auto rounded-br-md" : "mr-auto rounded-bl-md"
                )}
              >
                <img
                  src={msg.content}
                  alt="Shared image"
                  className="w-full h-auto"
                />
              </motion.div>
            )}
          </motion.div>
        ))}
      </div>

      {/* Input Bar */}
      {chatStatus === "expired" ? (
        <div className="px-4 py-5 border-t border-border/30 bg-background">
          <div className="text-center">
            <p className="text-sm text-muted-foreground">
              This conversation has ended.
            </p>
          </div>
        </div>
      ) : chatStatus === "request_sent" ? (
        <div className="px-4 py-5 border-t border-border/30 bg-background">
          <p className="text-sm text-muted-foreground text-center">
            You can still chat while waiting for a response
          </p>
        </div>
      ) : (
        <motion.div 
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          className="px-4 py-3 border-t border-border/30 bg-background"
        >
          <div className="flex items-center gap-2">
            <motion.button 
              whileHover={{ scale: 1.05 }}
              whileTap={hapticFeedback.medium}
              className="w-10 h-10 rounded-full flex items-center justify-center hover:bg-secondary/50 transition-colors"
            >
              <Image className="w-5 h-5 text-muted-foreground" />
            </motion.button>
            
            <Input
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              placeholder="Type a message..."
              className="flex-1 h-11 rounded-full border-border/50 bg-secondary/30 focus:bg-background px-4"
              onKeyDown={(e) => e.key === "Enter" && handleSend()}
            />
            
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={hapticFeedback.medium}
              onClick={handleSend}
              disabled={!message.trim()}
              className={cn(
                "w-10 h-10 rounded-full flex items-center justify-center transition-colors",
                message.trim()
                  ? "bg-primary text-primary-foreground"
                  : "bg-secondary/50 text-muted-foreground"
              )}
            >
              <Send className="w-5 h-5" />
            </motion.button>
          </div>
        </motion.div>
      )}

      {/* Continue Request Modal */}
      <ContinueRequestModal
        isOpen={showContinueModal}
        onClose={() => setShowContinueModal(false)}
        onSendRequest={handleSendContinueRequest}
        onAcceptRequest={handleAcceptContinueRequest}
        matchName={chatName}
        mode={chatStatus === "request_received" ? "received" : "send"}
      />
    </div>
  );
};
