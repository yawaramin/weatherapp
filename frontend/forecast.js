const Forecast = {
  view: (model, actions) =>
    h("div", { "class": "card" },
      h("header", { "class": "card-header" },
        h("p", { "class": "card-header-title" }, Cities[model.id])),

      h("div", { "class": "card-content" },
        h("div", { "class": "content" },
        model.days.map(day => day.dt))),

      h("footer", { "class": "card-footer" },
        h("a", { "class": "card-footer-item" }, "Remove")))
}
