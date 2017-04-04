const url_base = "http://localhost:8080";
const model = {
  // Initial selection is Dubai.
  forecasts: [], err: null, selected_city: 292223, is_loading: false
};

function make_set_error(actions, err) {
  return _ => actions.set_error(err);
}

function view(model, actions) {
  return h("div", {},
    model.err ?
      h("p", { "class": "notification is-warning" }, model.err) :
      h("p", { "class": "notification"},
        "Select and add cities to forecast below"),

    h("div", { "class": "field has-addons" },
      h("p", { "class": "control" },
        h("span", { "class": "select" },
          h("select",
            { onchange: e => actions.select_city(e.target.value) },
            Object.keys(Cities).map(city_id =>
              h("option", { value: city_id }, Cities[city_id]))))),

      h("p", { "class": "control" },
        h("a",
          { disabled: model.is_loading,
            onclick: _ => actions.add(null),
            "class": "button" },

          "Add City")),

      h("p", { "class": "control" },
        h("a",
          { disabled: model.is_loading,
            onclick: _ => actions.refresh(null),
            "class":
              `button is-primary${model.is_loading ? " is-loading" : ""}` },

          "Get Weather Data"))),

    h("div", {}, model.forecasts.map(f => Forecast.view(f, actions))));
}

const actions = {
  select_city: (_, city_id) => ({ selected_city: city_id, err: null }),
  set_error: (_, err) => ({ err }),
  set_loading: (_, is_loading) => ({ is_loading }),
  set_forecasts: (_, forecasts) => ({ err: null, forecasts }),
  add_forecast: (model, forecast) =>
    forecast ?
      { err: null,
        forecasts: model.forecasts.concat([forecast]) } :

      model,

  add: (model, _, actions) => {
    const set_error = make_set_error(actions, "Could not add city");

    return fetch(
      `${url_base}/add/${model.selected_city}`,
      { method: "POST" }).then(resp =>
      resp.ok ?
        resp.json().then(forecast => actions.add_forecast(forecast)) :
        set_error(null),

      set_error);
  },

  remove_city: (model, city_id) =>
    ({ err: null, forecasts: model.forecasts.filter(forecast =>
      forecast.id !== city_id) }),

  remove: (_, city_id, actions) => {
    const set_error = make_set_error(actions, "Could not remove city");

    return fetch(
      `${url_base}/remove/${city_id}`,
      { method: "POST" }).then(resp =>
      resp.ok ? actions.remove_city(city_id) : set_error(null),

      set_error);
  },

  refresh: (_, __, actions) => {
    const set_error =
      make_set_error(actions, "Could not get weather data");

    return fetch(`${url_base}/forecasts`).then(resp =>
    resp.ok ?
      resp.json().then(forecasts => actions.set_forecasts(forecasts)) :
      set_error(null),

    set_error);
  }
};

const subscriptions = [
  (model, actions) => {
    const set_error =
      make_set_error(actions, "Could not get forecast data");

    actions.set_loading(true);

    return fetch(`${url_base}/forecast_data`).then(resp =>
    resp.ok ?
      resp.json().then(forecasts => actions.set_forecasts(forecasts)) :
      set_error(null),

    set_error).then(_ =>
    actions.set_loading(false));
  }
];

app({ model, view, actions, subscriptions });
