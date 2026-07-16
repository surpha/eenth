import { Link } from "react-router-dom";
import { Globe, Mail } from "lucide-react";
import { brand, footerLinks } from "../data/mock";

export default function Footer() {
  return (
    <footer className="bg-[#1a1a1a] text-[#e6e0d1] pt-16 pb-10">
      <div className="max-w-[1360px] mx-auto px-6 md:px-10">
        <div className="grid md:grid-cols-12 gap-10">
          <div className="md:col-span-5">
            <div className="flex items-center gap-2 mb-5">
              <span className="font-sans-tight font-semibold text-[22px]">{brand.name}</span>
            </div>
            <p className="font-display text-[36px] md:text-[46px] leading-[1] max-w-md">Lock in. Live more.</p>
            <p className="mt-5 text-[14px] text-[#b8b0a0] max-w-md leading-relaxed">
              BLOCK is designed and shipped in India. A quiet nudge back to what matters.
            </p>
            <div className="mt-6 flex items-center gap-4 text-[#e6e0d1]">
              <a href="https://instagram.com" target="_blank" rel="noopener noreferrer" aria-label="Instagram" className="hover:opacity-70">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="2" y="2" width="20" height="20" rx="5" ry="5"/><path d="M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z"/><line x1="17.5" y1="6.5" x2="17.51" y2="6.5"/></svg>
              </a>
              <a href="https://x.com" target="_blank" rel="noopener noreferrer" aria-label="X / Twitter" className="hover:opacity-70">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/></svg>
              </a>
              <a href="https://youtube.com" target="_blank" rel="noopener noreferrer" aria-label="YouTube" className="hover:opacity-70">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M2.5 17a24.12 24.12 0 0 1 0-10 2 2 0 0 1 1.4-1.4 49.56 49.56 0 0 1 16.2 0A2 2 0 0 1 21.5 7a24.12 24.12 0 0 1 0 10 2 2 0 0 1-1.4 1.4 49.55 49.55 0 0 1-16.2 0A2 2 0 0 1 2.5 17"/><path d="m10 15 5-3-5-3z"/></svg>
              </a>
              <a href="mailto:hello@block.in" aria-label="Email" className="hover:opacity-70"><Mail size={18} /></a>
            </div>
          </div>

          <div className="md:col-span-7 grid grid-cols-2 md:grid-cols-3 gap-8">
            {Object.entries(footerLinks).map(([group, links]) => (
              <div key={group}>
                <div className="text-[12px] uppercase tracking-[0.16em] text-[#a89f8c] mb-4">{group}</div>
                <ul className="space-y-3">
                  {links.map((l) => (
                    <li key={l.label}>
                      {l.href.startsWith("/") ? (
                        <Link to={l.href} className="text-[14.5px] hover-underline">{l.label}</Link>
                      ) : (
                        <a href={l.href} className="text-[14.5px] hover-underline">{l.label}</a>
                      )}
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </div>

        <div className="mt-14 pt-6 border-t border-[#2b2b2b] flex flex-col md:flex-row md:items-center md:justify-between gap-4 text-[12.5px] text-[#a89f8c]">
          <div>© {new Date().getFullYear()} {brand.name}. Made in India.</div>
          <div className="flex items-center gap-5">
            <a href="#privacy" className="hover-underline">Privacy</a>
            <a href="#terms" className="hover-underline">Terms</a>
            <a href="#refunds" className="hover-underline">Refunds</a>
          </div>
        </div>
      </div>
    </footer>
  );
}
