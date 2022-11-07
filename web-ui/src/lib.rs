#![allow(clippy::wildcard_imports)]
// @TODO: Remove.
#![allow(dead_code, unused_variables)]

use seed::{prelude::*, *};
use serde::{Deserialize, Serialize};

// ------ ------
//     Init
// ------ ------

fn init(url: Url, orders: &mut impl Orders<Msg>) -> Model {
    orders.perform_cmd(async { Msg::RulesFetched(get_matched_rules().await) });
    Model {
        menu_visible: false,
        rules: vec![],
        selected_rule: None,
        top_tweets: vec![],
        number_of_tweets: 12,
        days_in_past: 7,
    }
}

// ------ ------
//     Model
// ------ ------
#[derive(Serialize, Deserialize, Clone)]
struct MatchedRule {
    id: String,
    tag: String,
    number_of_matches: u32,
}

#[derive(Serialize, Deserialize)]
struct TopTweet {
    tweet_id: String,
    number_of_refs: u32,
}

#[derive(Serialize, Deserialize)]
struct TopTweetsRequest {
    days_in_past: u16,
    limit: u16,
    rule_ids: Vec<String>,
}

struct Model {
    menu_visible: bool,
    rules: Vec<MatchedRule>,
    selected_rule: Option<MatchedRule>,
    top_tweets: Vec<TopTweet>,
    number_of_tweets: u16,
    days_in_past: u16,
}

// ------ ------
//    Update
// ------ ------

enum Msg {
    RulesFetched(fetch::Result<Vec<MatchedRule>>),
    TopTweetsFetched(fetch::Result<Vec<TopTweet>>),
    RuleSelected(String),
    ToggleMenu,
    HideMenu,
}

fn update(msg: Msg, model: &mut Model, orders: &mut impl Orders<Msg>) {
    match msg {
        Msg::ToggleMenu => model.menu_visible = not(model.menu_visible),
        Msg::HideMenu => {
            if model.menu_visible {
                model.menu_visible = false;
            } else {
                orders.skip();
            }
        }
        Msg::RulesFetched(Ok(rules)) => {
            if !rules.is_empty() {
                let selected_rule = rules[0].clone();
                let selected_rule2 = rules[0].clone();
                model.rules = rules;
                model.selected_rule = Some(selected_rule);
                let req = TopTweetsRequest {
                    limit: model.number_of_tweets,
                    days_in_past: model.days_in_past,
                    rule_ids: vec![selected_rule2.id],
                };
                orders.perform_cmd(async { Msg::TopTweetsFetched(get_top_tweets(req).await) });
            }
        }
        Msg::RulesFetched(Err(err)) => {
            log!("Error fetching rules", err);
        }
        Msg::TopTweetsFetched(Ok(tweats)) => {
            model.top_tweets = tweats;
        }
        Msg::TopTweetsFetched(Err(err)) => {
            log!("Error fetching toptweets", err);
        }
        Msg::RuleSelected(_) => {}
    }
}

async fn get_top_tweets(request: TopTweetsRequest) -> fetch::Result<Vec<TopTweet>> {
    Request::new("/api/tw/top")
        .method(Method::Post)
        .json(&request)?
        .fetch()
        .await?
        .check_status()?
        .json::<Vec<TopTweet>>()
        .await
}

async fn get_matched_rules() -> fetch::Result<Vec<MatchedRule>> {
    Request::new("/api/tw/matchedrules")
        .method(Method::Get)
        .fetch()
        .await?
        .check_status()?
        .json::<Vec<MatchedRule>>()
        .await
}

// ------ ------
//     View
// ------ ------

fn view(model: &Model) -> Vec<Node<Msg>> {
    vec![view_navbar(model), view_content(model)]
}

// ----- view_content ------

fn view_content(model: &Model) -> Node<Msg> {
    div![C!["container"],]
}

// ----- view_navbar ------

fn view_navbar(model: &Model) -> Node<Msg> {
    nav![
        C!["navbar", "is-link"],
        attrs! {
            At::from("role") => "navigation",
            At::AriaLabel => "main navigation",
        },
        div![
            C!["navbar-brand"],
            // ------ Logo ------
            a![
                C!["navbar-item", "has-text-weight-bold", "is-size-3"],
                "SMA - ONLY FOR DEMO PURPOSE!"
            ],
            // ------ Hamburger ------1000MyGromova
            a![
                C![
                    "navbar-burger",
                    "burger",
                    IF!(model.menu_visible => "is-active")
                ],
                style! {
                    St::MarginTop => "auto",
                    St::MarginBottom => "auto",
                },
                attrs! {
                    At::from("role") => "button",
                    At::AriaLabel => "menu",
                    At::AriaExpanded => model.menu_visible,
                },
                ev(Ev::Click, |event| {
                    event.stop_propagation();
                    Msg::ToggleMenu
                }),
                span![attrs! {At::AriaHidden => "true"}],
                span![attrs! {At::AriaHidden => "true"}],
                span![attrs! {At::AriaHidden => "true"}],
            ]
        ],
        div![
            C!["navbar-menu", IF!(model.menu_visible => "is-active")],
            div![C!["navbar-start"],],
            div![
                C!["navbar-end"],
                div![
                    C!["navbar-item"],
                    div![
                        C!["buttons"],
                        // a![
                        //     C!["button", "is-primary"],
                        //     attrs![
                        //         At::Href => Urls::new(base_url).settings(),
                        //     ],
                        //     strong![&user.nickname],
                        // ],
                        // a![
                        //     C!["button", "is-light"],
                        //     "Log out",
                        //     ev(Ev::Click, |_| Msg::LogOut),
                        // ]
                    ]
                ]
            ]
        ]
    ]
}

// ------ ------
//     Start
// ------ ------

#[wasm_bindgen(start)]
pub fn start() {
    App::start("app", init, update, view);
}
