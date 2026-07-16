import { useState } from "react";
import { ChevronDown } from "lucide-react";
import { faqs } from "../data/mock";

function AccordionItem({ faq }) {
  const [open, setOpen] = useState(false);

  return (
    <div>
      <button
        className="accordion-trigger"
        data-open={open}
        onClick={() => setOpen(!open)}
      >
        <span>{faq.q}</span>
        <ChevronDown size={18} />
      </button>
      {open && (
        <div className="accordion-content">
          {faq.a}
        </div>
      )}
    </div>
  );
}

export default function FAQ({ limit }) {
  const list = limit ? faqs.slice(0, limit) : faqs;
  return (
    <section id="faq" className="py-20 md:py-28">
      <div className="max-w-[1100px] mx-auto px-6 md:px-10">
        <div className="text-center mb-10">
          <p className="chip mb-4">FAQ</p>
          <h2 className="section-title text-[44px] md:text-[60px]">Good questions.<br />Straight answers.</h2>
        </div>
        <div className="w-full">
          {list.map((f, i) => (
            <AccordionItem key={i} faq={f} />
          ))}
        </div>
      </div>
    </section>
  );
}
