import { Check, X } from "lucide-react";
import { comparison } from "../data/mock";

export default function Comparison() {
  return (
    <section className="py-20 md:py-28 bg-[#ece5d8]">
      <div className="max-w-[1360px] mx-auto px-6 md:px-10">
        <div className="max-w-2xl">
          <h2 className="section-title text-[44px] md:text-[64px]">
            What makes BLOCK<br />different.
          </h2>
          <p className="mt-5 text-[#3a3a3a] text-[15.5px]">
            You can't just tap "disable" and get sucked back in. With other solutions, the "key" that re-enables everything is always in your pocket. BLOCK is different.
          </p>
        </div>

        <div className="mt-10 md:mt-14 overflow-x-auto no-scrollbar">
          <table className="w-full min-w-[720px] border-separate border-spacing-0">
            <thead>
              <tr>
                <th className="text-left text-[13px] font-medium text-[#6b6b6b] pb-4 pr-4">Feature</th>
                {comparison.columns.map((c) => (
                  <th
                    key={c.key}
                    className={`text-center text-[13px] font-medium pb-4 px-3 ${c.highlight ? "text-[#1a1a1a]" : "text-[#6b6b6b]"}`}
                  >
                    <div className={`inline-flex items-center justify-center px-3 py-1 rounded-full ${c.highlight ? "bg-[#1a1a1a] text-[#f2ede4]" : "bg-transparent"}`}>
                      {c.label}
                    </div>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {comparison.rows.map((row, i) => (
                <tr key={i} className="border-t border-[#d9d1c1]">
                  <td className="py-4 pr-4 text-[14.5px] text-[#1a1a1a] border-t border-[#d9d1c1]">{row.feature}</td>
                  {comparison.columns.map((c) => (
                    <td key={c.key} className={`text-center py-4 px-3 border-t border-[#d9d1c1] ${c.highlight ? "bg-[#f2ede4]" : ""}`}>
                      {row.values[c.key] ? (
                        <span className="check-mark"><Check size={14} strokeWidth={2.5} /></span>
                      ) : (
                        <span className="cross-mark"><X size={14} strokeWidth={2.5} /></span>
                      )}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="mt-10 text-center">
          <a href="#buy" className="btn-ink">Get BLOCK — ₹999</a>
        </div>
      </div>
    </section>
  );
}
