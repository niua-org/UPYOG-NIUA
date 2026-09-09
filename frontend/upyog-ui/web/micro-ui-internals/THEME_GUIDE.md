# UPYOG Dynamic Theme & Design System Guide

This guide explains how styling, design, and dynamic theming work across UPYOG.

---

## 1. Quick Example

### In React Components (Tailwind Classes)
```jsx
import React from "react";

export const CustomCard = ({ title, description }) => {
  return (
    <div className="bg-white border border-border p-md rounded-md shadow-sm">
      <h2 className="text-xl font-bold text-primary-main">{title}</h2>
      <p className="text-sm text-text-secondary mt-xs">{description}</p>
      <button className="bg-primary-main text-white px-md py-sm rounded-sm font-medium hover:bg-primary-dark">
        Submit
      </button>
    </div>
  );
};
```

### In SCSS / CSS Stylesheets
```scss
.custom-header {
  background: var(--gradient-primary);
  color: var(--white);
  padding: var(--spacing-md);
  border-radius: var(--border-radius-lg);
  font-size: var(--font-size-xl);
  font-weight: var(--weight-bold);
}
```

---

## 2. Design Token Mapping Reference

All tokens defined in `tailwind.theme.json` or returned from the MDMS Theme API automatically produce these utility classes and CSS variables:

### 🎨 Colors
| JSON Path | Tailwind Background | Tailwind Text | Tailwind Border | CSS Custom Property |
| :--- | :--- | :--- | :--- | :--- |
| `colors.primary.main` | `bg-primary-main` | `text-primary-main` | `border-primary-main` | `var(--primary-main)` |
| `colors.primary.light` | `bg-primary-light` | `text-primary-light` | `border-primary-light` | `var(--primary-light)` |
| `colors.primary.dark` | `bg-primary-dark` | `text-primary-dark` | `border-primary-dark` | `var(--primary-dark)` |
| `colors.secondary` | `bg-secondary` | `text-secondary` | `border-secondary` | `var(--secondary)` |
| `colors.text.primary` | `bg-text-primary` | `text-text-primary` | — | `var(--text-primary)` |
| `colors.text.secondary` | `bg-text-secondary`| `text-text-secondary` | — | `var(--text-secondary)` |
| `colors.border` | — | — | `border-border` | `var(--border)` |
| `colors.success` | `bg-success` | `text-success` | `border-success` | `var(--success)` |
| `colors.error` | `bg-error` | `text-error` | `border-error` | `var(--error)` |
| `colors.white` | `bg-white` | `text-white` | `border-white` | `var(--white)` |
| `colors.grey.light` | `bg-grey-light` | `text-grey-light` | `border-grey-light` | `var(--grey-light)` |

### 📏 Spacing & Sizing
| JSON Path | Padding Class | Margin Class | Gap Class | CSS Variable |
| :--- | :--- | :--- | :--- | :--- |
| `spacing.xs` (`4px`) | `p-xs` / `px-xs` / `py-xs` | `m-xs` / `mt-xs` / `mb-xs` | `gap-xs` | `var(--spacing-xs)` |
| `spacing.sm` (`8px`) | `p-sm` / `px-sm` / `py-sm` | `m-sm` / `mt-sm` / `mb-sm` | `gap-sm` | `var(--spacing-sm)` |
| `spacing.md` (`16px`)| `p-md` / `px-md` / `py-md` | `m-md` / `mt-md` / `mb-md` | `gap-md` | `var(--spacing-md)` |
| `spacing.lg` (`24px`)| `p-lg` / `px-lg` / `py-lg` | `m-lg` / `mt-lg` / `mb-lg` | `gap-lg` | `var(--spacing-lg)` |
| `spacing.xl` (`32px`)| `p-xl` / `px-xl` / `py-xl` | `m-xl` / `mt-xl` / `mb-xl` | `gap-xl` | `var(--spacing-xl)` |

### 🔲 Borders & Radii
| JSON Path | Tailwind Class | CSS Variable |
| :--- | :--- | :--- |
| `borderRadius.sm` (`4px`) | `rounded-sm` | `var(--border-radius-sm)` |
| `borderRadius.md` (`8px`) | `rounded-md` | `var(--border-radius-md)` |
| `borderRadius.lg` (`16px`) | `rounded-lg` | `var(--border-radius-lg)` |
| `borderRadius.xl` (`24px`) | `rounded-xl` | `var(--border-radius-xl)` |
| `borderWidth.sm` (`1px`) | `border-sm` / `border` | `var(--border-width-sm)` |
| `borderWidth.md` (`2px`) | `border-md` | `var(--border-width-md)` |

### 🔤 Typography
| JSON Path | Tailwind Class | CSS Variable |
| :--- | :--- | :--- |
| `fontSize.xs` (`12px`) | `text-xs` | `var(--font-size-xs)` |
| `fontSize.sm` (`14px`) | `text-sm` | `var(--font-size-sm)` |
| `fontSize.md` (`16px`) | `text-md` | `var(--font-size-md)` |
| `fontSize.lg` (`18px`) | `text-lg` | `var(--font-size-lg)` |
| `fontSize.xl` (`20px`) | `text-xl` | `var(--font-size-xl)` |
| `fontWeight.regular` (`400`) | `font-normal` | `var(--weight-regular)` |
| `fontWeight.medium` (`500`) | `font-medium` | `var(--weight-medium)` |
| `fontWeight.bold` (`700`) | `font-bold` | `var(--weight-bold)` |

---

## 3. How Dynamic Theming Works

1. **Build Time**:
   - `tailwind.config.js` reads `tailwind.theme.json` to generate all utility classes and injects default variables into `:root`.
2. **Runtime**:
   - `useThemeConfig` fetches the tenant's theme from `/mdms-v2/v1/theme-config/_search`.
   - `ThemeService.applyTheme(response)` updates the CSS variables on `<html>` dynamically.
   - If the API fails or returns no data, the default styles from `tailwind.theme.json` automatically apply.
