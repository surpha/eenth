import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { ShoppingBag, Menu, X } from "lucide-react";
import { brand } from "../data/mock";

const LOGO_MARK = (
  <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
    <path d="M7 11V7a5 5 0 0 1 10 0v4" />
  </svg>
);

export default function Navbar() {
  const [scrolled, setScrolled] = useState(false);
  const [open, setOpen] = useState(false);
  const { pathname } = useLocation();

  useEffect(() => {
    const on = () => setScrolled(window.scrollY > 8);
    on();
    window.addEventListener("scroll", on);
    return () => window.removeEventListener("scroll", on);
  }, []);

  const nav = [
    { label: "Home", href: "/" },
    { label: "Our Story", href: "/our-story" },
    { label: "FAQ", href: "/faq" },
  ];

  return (
    <header className={`fixed top-0 left-0 right-0 z-40 transition-all ${scrolled ? "backdrop-blur-md bg-[#f2ede4]/80 border-b border-[#e0d8c7]" : "bg-transparent"}`}>
      <div className="max-w-[1360px] mx-auto px-6 md:px-10">
        <div className="h-16 md:h-20 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2 text-[#1a1a1a]">
            {LOGO_MARK}
            <span className="font-sans-tight font-semibold text-[19px] tracking-tight">{brand.name}</span>
          </Link>

          <nav className="hidden md:flex items-center gap-8">
            {nav.map((n) => {
              const active = n.href === pathname;
              return (
                <Link key={n.label} to={n.href} className={`text-[14px] hover-underline ${active ? "text-[#1a1a1a]" : "text-[#3a3a3a]"}`}>
                  {n.label}
                </Link>
              );
            })}
          </nav>

          <div className="flex items-center gap-3">
            <a href="#buy" className="btn-ink text-[13.5px] hidden md:inline-flex">Shop Now</a>
            <button onClick={() => setOpen((v) => !v)} className="md:hidden text-[#1a1a1a]">
              {open ? <X size={22} /> : <Menu size={22} />}
            </button>
          </div>
        </div>
      </div>

      {open && (
        <div className="md:hidden border-t border-[#e0d8c7] bg-[#f2ede4]">
          <div className="px-6 py-4 flex flex-col gap-4">
            {nav.map((n) => (
              <Link key={n.label} to={n.href} onClick={() => setOpen(false)} className="text-[15px]">{n.label}</Link>
            ))}
            <a href="#buy" onClick={() => setOpen(false)} className="btn-ink text-center">Shop Now — ₹{brand.price}</a>
          </div>
        </div>
      )}
    </header>
  );
}
