import { useEffect } from "react";
import "./App.css";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import Home from "./pages/Home";
import OurStory from "./pages/OurStory";
import FAQPage from "./pages/FAQPage";

function App() {
  useEffect(() => {
    document.title = "BLOCK — Lock In";
  }, []);

  return (
    <div className="App">
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/our-story" element={<OurStory />} />
          <Route path="/faq" element={<FAQPage />} />
        </Routes>
      </BrowserRouter>
    </div>
  );
}

export default App;
