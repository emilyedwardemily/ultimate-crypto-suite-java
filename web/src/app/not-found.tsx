import Link from "next/link";

export default function NotFound() {
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center text-center">
      <h1 className="text-6xl font-bold text-crypto-accent">404</h1>
      <p className="mt-4 text-lg text-crypto-text-muted">
        Page not found. The cipher you seek does not exist.
      </p>
      <Link
        href="/"
        className="btn-primary mt-6 inline-block"
      >
        Return to Dashboard
      </Link>
    </div>
  );
}
