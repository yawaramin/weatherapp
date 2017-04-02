const url_base = "localhost:8080";
const init_model = {
  forecasts: [], error: {}, selected_city: {}, is_loading: false
};

function view(model, actions) {
  return h("div", {},
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
            onclick: _ => actions.add(model.selected_city),
            "class": "button" },

          "Add City")),

      h("p", { "class": "control" },
        h("a",
          { disabled: model.is_loading,
            onclick: _ => actions.refresh,
            "class":
              "button is-primary" +
                (model.is_loading ? " is-loading" : "") },

          "Refresh"))),

    h("div", {}, model.forecasts.map(f => Forecast.view(f, actions))));
}

app({ model: init_model, view });
