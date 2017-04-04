const url_base = "http://localhost:8080";
const model = {
  // Initial selection is Dubai.
  forecasts: [], err: null, selected_city: 292223, is_loading: false
};

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
            onclick: _ => actions.refresh,
            "class":
              `button is-primary${model.is_loading ? " is-loading" : ""}` },

          "Refresh"))),

    h("div", {},
      model.forecasts.map(f => Forecast.view(f, Forecast.actions))));
}

const actions = {
  select_city: (_, city_id) => ({ selected_city: city_id, err: null }),
  set_error: (_, err) => ({ err }),
  add_forecast: (model, forecast) =>
    forecast ?
      { err: null,
        forecasts: model.forecasts.concat([forecast]) } :

      model,

  add: (model, _, actions) => {
    const err = "Could not add city";

    return fetch(
      `${url_base}/add/${model.selected_city}`, { method: "POST" })
      .then(
        resp =>
          resp.ok ?
            resp
              .json().then(forecast => actions.add_forecast(forecast)) :

            actions.set_error(err),

        _ => actions.set_error(err));
  }
};

app({ model, view, actions });
