import { useState } from "react";
import { modes, productImages } from "../data/mock";

export default function Modes() {
  const [active, setActive] = useState(modes[0].id);
  const current = modes.find((m) => m.id === active);

  return (
    <section className="py-20 md:py-28">
      <div className="max-w-[1360px] mx-auto px-6 md:px-10">
        <div className="grid md:grid-cols-12 gap-8 md:gap-12">
          <div className="md:col-span-5">
            <h2 className="section-title text-[44px] md:text-[64px]">
              Get more<br />from your phone.
            </h2>
            <p className="mt-5 text-[#3a3a3a] text-[15.5px] max-w-md">
              Build custom tech routines that match your life. Tap BLOCK to switch between focus, family, sleep, and everything in between.
            </p>
            <div className="mt-8 flex flex-wrap gap-2">
              {modes.map((m) => (
                <button
                  key={m.id}
                  onClick={() => setActive(m.id)}
                  className={`px-4 py-2 rounded-full text-[13px] border transition-colors ${active === m.id ? "bg-[#1a1a1a] text-[#f2ede4] border-[#1a1a1a]" : "bg-transparent text-[#1a1a1a] border-[#1a1a1a]/25 hover:border-[#1a1a1a]"}`}
                >
                  {m.label}
                </button>
              ))}
            </div>
          </div>

          <div className="md:col-span-7">
            <div className="relative rounded-[28px] overflow-hidden aspect-[16/11] grain">
              <img
                key={current.image}
                src={productImages[current.image]}
                alt={current.title}
                className="w-full h-full object-cover tilt-in"
              />
              <div className="absolute inset-x-0 bottom-0 p-6 md:p-8 bg-gradient-to-t from-black/55 to-transparent">
                <h3 className="font-display text-[30px] md:text-[36px] text-white">{current.title}</h3>
                <p className="mt-2 text-white/85 text-[14.5px] max-w-lg">{current.description}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
