export const ThemeConfig = [
  {
    tenantId: "pg",
    themeType: "EMPLOYEE",
    isActive: true,
    status: "DEFAULT",
    workflowId: null,
    config: {
      theme: {
        screens: {
          dt: "780px",
        },
        extend: {
          spacing: {
            xs: "4px",
            sm: "8px",
            md: "16px",
            lg: "24px",
            xl: "32px",
            "2xl": "40px",
            "1/2": "50%",
            "1/3": "33.333333%",
            "1/4": "25%",
            "3/4": "75%",
          },
          colors: {
            primary: {
              light: "#F18F5E",
              main: "#a82227",
              dark: "#C8602B",
            },

            secondary: "#22394D",

            text: {
              primary: "#0B0C0C",
              secondary: "#505A5F",
            },
            link: {
              normal: "#1D70B8",
              hover: "#003078",
            },
            border: "#D6D5D4",
            inputBorder: "#464646",
            focus: "#a82227",
            error: "#D4351C",
            success: "#00703C",
            black: "#000000",
            grey: {
              dark: "#9E9E9E",
              mid: "#EEEEEE",
              light: "#FAFAFA",
              bg: "#E3E3E3",
            },
            white: "#FFFFFF",
          },
          gradient: {
            primary: "linear-gradient(to right, #a82227, #C8602B)",
            secondary: "linear-gradient(to right, #22394D, #505A5F)",
            tertiary: "linear-gradient(to right, #505A5F, #9E9E9E)",
            quaternary: "linear-gradient(to right, #9E9E9E, #EEEEEE)",
            quinary: "linear-gradient(to right, #EEEEEE, #FAFAFA)",
            senary: "linear-gradient(to right, #FAFAFA, #FFFFFF)",
          },
          fontFamily: {
            sans: ["Roboto", "sans-serif"],
            rc: ['"Roboto Condensed"', "sans-serif"],
          },
          fontWeight: {
            regular: "400",
            medium: "500",
            bold: "700",
          },
          fontSize: {
            xs: "12px",
            sm: "14px",
            md: "16px",
            lg: "18px",
            xl: "20px",
            "2xl": "24px",
          },
          textAlign: {
            left: "left",
            center: "center",
            right: "right",
            justify: "justify",
          },

          borderRadius: {
            sm: "4px",
            md: "8px",
            lg: "16px",
            xl: "24px",
            "2xl": "32px",
          },
          borderWidth: {
            sm: "1px",
            md: "2px",
            lg: "4px",
            xl: "8px",
            "2xl": "16px",
          },
          borderStyle: {
            solid: "solid",
            dashed: "dashed",
            dotted: "dotted",
            double: "double",
            groove: "groove",
            ridge: "ridge",
            inset: "inset",
            outset: "outset",
          },
          borderColor: {
            primary: "#a82227",
            secondary: "#22394D",
            tertiary: "#505A5F",
            quaternary: "#9E9E9E",
            quinary: "#EEEEEE",
            senary: "#FAFAFA",
          },
          boxShadow: {
            sm: "0 1px 2px 0 rgba(0, 0, 0, 0.05)",
            md: "0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)",
            lg: "0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)",
            xl: "0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)",
            "2xl": "0 25px 50px -12px rgba(0, 0, 0, 0.25)",
          },
          boxShadowColor: {
            primary: "#a82227",
            secondary: "#22394D",
            tertiary: "#505A5F",
            quaternary: "#9E9E9E",
            quinary: "#EEEEEE",
            senary: "#FAFAFA",
          },
          boxShadowOpacity: {
            sm: "0.05",
            md: "0.1",
            lg: "0.15",
            xl: "0.2",
            "2xl": "0.25",
          },
          button: {
            primary: {
              background: "#a82227",
              color: "#FFFFFF",
            },
            secondary: {
              background: "#22394D",
              color: "#FFFFFF",
            },
            tertiary: {
              background: "#505A5F",
              color: "#FFFFFF",
            },
            inverse: {
              background: "#FFFFFF",
              color: "#000000",
            },
          },
          pageContainer: {
            defaultColumns: 1,
            gap: "16px",
            layouts: [
              {
                key: "1",
                columns: 1,
                template: "1fr",
              },
              {
                key: "2",
                columns: 2,
                template: "1fr 1fr",
              },
              {
                key: "3",
                columns: 3,
                template: "1fr 1fr 1fr",
              },
            ],
          },
        },
      },
    },
  },
];

