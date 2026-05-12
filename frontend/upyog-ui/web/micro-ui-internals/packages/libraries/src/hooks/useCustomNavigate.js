import { useNavigate } from "react-router-dom";

/**
 * Custom navigation hook that wraps react-router-dom's useNavigate
 * This centralizes navigation logic for easier upgrades
 * 
 * Usage:
 * const navigate = Digit.Hooks.useCustomNavigate();
 * navigate("/path");
 * navigate("/path", { state: { data } });
 * navigate(-1); // go back
 */
const useCustomNavigate = () => {
  try {
    const navigate = useNavigate(); // This will throw if outside Router
    return navigate;
  } catch (error) {
    // Return fallback when outside Router context
    return (to) => {
      if (typeof to === "string") {
        window.location.href = to;
      }
    };
  }
};

export default useCustomNavigate;
