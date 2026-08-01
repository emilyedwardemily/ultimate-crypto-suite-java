export default function Loading({ text = "Loading..." }: { text?: string }) {
  return (
    <div className="flex min-h-[40vh] flex-col items-center justify-center gap-4">
      <div className="h-8 w-8 animate-spin rounded-full border-2 border-crypto-accent border-t-transparent" />
      <p className="text-sm text-crypto-text-muted">{text}</p>
    </div>
  );
}
