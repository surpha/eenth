import { pressLogos } from "../data/mock";

export default function PressBar() {
  const items = [...pressLogos, ...pressLogos];
  return (
    <section className="py-10 md:py-12 border-y border-[#e0d8c7] bg-[#ece5d8]">
      <div className="max-w-[1360px] mx-auto px-6 md:px-10">
        <p className="text-center text-[11.5px] uppercase tracking-[0.18em] text-[#6b6b6b] mb-6">As featured in</p>
        <div className="marquee">
          <div className="marquee-track">
            {items.map((p, i) => (
              <span key={i} className="font-display text-[22px] md:text-[26px] text-[#3a3a3a] whitespace-nowrap opacity-80">
                {p}
              </span>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
