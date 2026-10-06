use serde::{Deserialize, Serialize};

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq, Serialize, Deserialize)]
pub enum ThemePreference {
    #[default]
    SystemDefault,
    BambooGroveLight,
    MoonlitBambooDark,
    ForestTechLight,
    ForestTechDark,
    PandaWavePink,
}

impl ThemePreference {
    pub const SYSTEM_DEFAULT_WIRE: &'static str = "system_default";
    pub const BAMBOO_GROVE_LIGHT_WIRE: &'static str = "bamboo_grove_light";
    pub const MOONLIT_BAMBOO_DARK_WIRE: &'static str = "moonlit_bamboo_dark";
    pub const FOREST_TECH_LIGHT_WIRE: &'static str = "forest_tech_light";
    pub const FOREST_TECH_DARK_WIRE: &'static str = "forest_tech_dark";
    pub const PANDAWAVE_PINK_WIRE: &'static str = "pandawave_pink";

    pub fn from_wire(value: &str) -> Option<Self> {
        match value {
            Self::SYSTEM_DEFAULT_WIRE => Some(Self::SystemDefault),
            Self::BAMBOO_GROVE_LIGHT_WIRE => Some(Self::BambooGroveLight),
            Self::MOONLIT_BAMBOO_DARK_WIRE => Some(Self::MoonlitBambooDark),
            Self::FOREST_TECH_LIGHT_WIRE => Some(Self::ForestTechLight),
            Self::FOREST_TECH_DARK_WIRE => Some(Self::ForestTechDark),
            Self::PANDAWAVE_PINK_WIRE => Some(Self::PandaWavePink),
            _ => None,
        }
    }

    pub fn as_wire(self) -> &'static str {
        match self {
            Self::SystemDefault => Self::SYSTEM_DEFAULT_WIRE,
            Self::BambooGroveLight => Self::BAMBOO_GROVE_LIGHT_WIRE,
            Self::MoonlitBambooDark => Self::MOONLIT_BAMBOO_DARK_WIRE,
            Self::ForestTechLight => Self::FOREST_TECH_LIGHT_WIRE,
            Self::ForestTechDark => Self::FOREST_TECH_DARK_WIRE,
            Self::PandaWavePink => Self::PANDAWAVE_PINK_WIRE,
        }
    }
}

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq, Serialize, Deserialize)]
pub enum PreferenceSource {
    #[default]
    Uninitialized,
    LocalCache,
    LocalUser,
    RemoteProfile,
}

impl PreferenceSource {
    pub fn as_wire(self) -> &'static str {
        match self {
            Self::Uninitialized => "uninitialized",
            Self::LocalCache => "local_cache",
            Self::LocalUser => "local_user",
            Self::RemoteProfile => "remote_profile",
        }
    }
}

#[derive(Clone, Debug, Default, Eq, PartialEq, Serialize, Deserialize)]
pub struct ThemePreferenceState {
    pub theme: ThemePreference,
    pub source: PreferenceSource,
    pub revision: u64,
    pub session_user_id: Option<String>,
}

impl ThemePreferenceState {
    pub fn is_initialized(&self) -> bool {
        self.source != PreferenceSource::Uninitialized
    }
}

/// Applies application-owned preference values without discarding keys that a newer server
/// schema may have stored alongside them.
pub fn merge_preferences(
    current: serde_json::Value,
    updates: serde_json::Value,
) -> serde_json::Value {
    match (current, updates) {
        (serde_json::Value::Object(mut current), serde_json::Value::Object(updates)) => {
            for (key, update) in updates {
                let merged = current
                    .remove(&key)
                    .map(|existing| merge_preferences(existing, update.clone()))
                    .unwrap_or(update);
                current.insert(key, merged);
            }
            serde_json::Value::Object(current)
        }
        (_, updates) => updates,
    }
}

#[cfg(test)]
mod tests {
    use serde_json::json;

    use super::{merge_preferences, ThemePreference};

    #[test]
    fn pink_theme_survives_wire_and_storage_round_trip() {
        let theme = ThemePreference::from_wire("pandawave_pink").expect("pink theme is supported");
        assert_eq!(theme.as_wire(), "pandawave_pink");
        let stored = serde_json::to_string(&theme).unwrap();
        assert_eq!(
            serde_json::from_str::<ThemePreference>(&stored).unwrap(),
            theme
        );
    }

    #[test]
    fn updating_known_theme_preserves_unknown_keys() {
        let current = json!({"theme":"dark","future_key":{"nested":7}});
        let updated = merge_preferences(current, json!({"theme":"light"}));

        assert_eq!(updated["future_key"]["nested"], 7);
    }
}
