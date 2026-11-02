package com.rustbeatz.disasteralert.ui.screens.map

object LeafletMapHtml {

    /**
     * Builds the self-contained HTML page containing Leaflet 1.9.4, OpenStreetMap tiles,
     * and JavaScript functions window.setData(payload) and window.focusOn(replyId).
     */
    fun buildHtml(isDarkTheme: Boolean): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <meta name="referrer" content="strict-origin-when-cross-origin">
            <title>DisasterAlert Help Map</title>
            
            <!-- Leaflet 1.9.4 CSS from pinned CDN -->
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            
            <style>
                html, body, #map {
                    height: 100%;
                    width: 100%;
                    margin: 0;
                    padding: 0;
                    background-color: ${if (isDarkTheme) "#0B1412" else "#F3F7F5"};
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                }
                
                /* Custom marker styling */
                .custom-marker {
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    border-radius: 50%;
                    border: 2.5px solid #FFFFFF;
                    box-shadow: 0 2px 6px rgba(0,0,0,0.45);
                    cursor: pointer;
                    transition: transform 0.2s ease;
                }
                .custom-marker:hover {
                    transform: scale(1.15);
                }
                
                /* Popup styling */
                .leaflet-popup-content-wrapper {
                    background: ${if (isDarkTheme) "#13221E" else "#FFFFFF"};
                    color: ${if (isDarkTheme) "#F0F5F2" else "#111E1A"};
                    border-radius: 14px;
                    box-shadow: 0 4px 16px rgba(0,0,0,0.3);
                    border: 1px solid ${if (isDarkTheme) "#243D36" else "#D6E2DB"};
                    padding: 4px;
                }
                .leaflet-popup-content {
                    margin: 10px 12px;
                    line-height: 1.4;
                    font-size: 13px;
                }
                .leaflet-popup-tip {
                    background: ${if (isDarkTheme) "#13221E" else "#FFFFFF"};
                }
                
                .popup-title {
                    font-size: 15px;
                    font-weight: 700;
                    margin-bottom: 4px;
                    display: flex;
                    align-items: center;
                    gap: 6px;
                }
                .simulated-badge {
                    background: #FEF3C7;
                    color: #B45309;
                    font-size: 10px;
                    font-weight: 800;
                    padding: 2px 5px;
                    border-radius: 4px;
                    border: 1px solid #F59E0B;
                    letter-spacing: 0.5px;
                }
                .need-pill {
                    display: inline-block;
                    font-size: 11px;
                    font-weight: 700;
                    padding: 2px 8px;
                    border-radius: 12px;
                    color: #FFFFFF;
                    margin-bottom: 6px;
                }
                .popup-meta {
                    font-size: 11.5px;
                    color: ${if (isDarkTheme) "#96A8A0" else "#536B63"};
                    margin: 3px 0;
                }
                .popup-note {
                    font-size: 12px;
                    font-style: italic;
                    background: ${if (isDarkTheme) "#1B2F2A" else "#F3F7F5"};
                    padding: 6px 8px;
                    border-radius: 6px;
                    margin: 6px 0;
                    border-left: 3px solid #0D7C59;
                }
                .nav-btn {
                    display: inline-flex;
                    align-items: center;
                    gap: 6px;
                    margin-top: 8px;
                    padding: 6px 12px;
                    background-color: #0D7C59;
                    color: #FFFFFF !important;
                    text-decoration: none;
                    border-radius: 8px;
                    font-size: 12px;
                    font-weight: 700;
                    cursor: pointer;
                    box-shadow: 0 1px 3px rgba(0,0,0,0.2);
                }
                .nav-btn:active {
                    background-color: #064E3B;
                }
                
                /* Small Map Legend in bottom-right */
                .map-legend {
                    position: absolute;
                    bottom: 90px;
                    right: 12px;
                    z-index: 999;
                    background: ${if (isDarkTheme) "rgba(19, 34, 30, 0.92)" else "rgba(255, 255, 255, 0.92)"};
                    backdrop-filter: blur(6px);
                    border: 1px solid ${if (isDarkTheme) "#243D36" else "#D6E2DB"};
                    border-radius: 10px;
                    padding: 6px 10px;
                    font-size: 10.5px;
                    color: ${if (isDarkTheme) "#E2EBE6" else "#111E1A"};
                    box-shadow: 0 2px 8px rgba(0,0,0,0.15);
                    line-height: 1.5;
                }
                .legend-item {
                    display: flex;
                    align-items: center;
                    gap: 6px;
                    margin: 2px 0;
                }
                .legend-dot {
                    width: 9px;
                    height: 9px;
                    border-radius: 50%;
                    display: inline-block;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            
            <div class="map-legend">
                <div style="font-weight: 700; margin-bottom: 2px; font-size: 10px; color: ${if (isDarkTheme) "#A7F3D0" else "#0D7C59"};">LEGEND</div>
                <div class="legend-item"><span class="legend-dot" style="background:#991B1B;"></span> Emergency</div>
                <div class="legend-item"><span class="legend-dot" style="background:#DC2626;"></span> Medicine</div>
                <div class="legend-item"><span class="legend-dot" style="background:#EA580C;"></span> Food / Water</div>
                <div class="legend-item"><span class="legend-dot" style="background:#2563EB;"></span> Shelter</div>
                <div class="legend-item"><span class="legend-dot" style="background:#6B7280;"></span> Other / Unspec</div>
                <div class="legend-item"><span class="legend-dot" style="background:#10B981;"></span> Safe</div>
            </div>

            <!-- Leaflet 1.9.4 JS from pinned CDN -->
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            
            <script>
                // Single constant for Tile URL as required by S1 rules
                const TILE_URL = "https://tile.openstreetmap.org/{z}/{x}/{y}.png";
                
                // Default view: India centre [22.5, 79], zoom 5
                const map = L.map('map', { 
                    zoomControl: false,
                    attributionControl: false 
                }).setView([22.5, 79], 5);
                window.map = map;
                
                window.refreshMap = function() {
                    try {
                        if (window.map) {
                            window.map.invalidateSize();
                        }
                    } catch(e) {
                        console.error("refreshMap error: " + e);
                    }
                };
                
                // Add OpenStreetMap tile layer with exact attribution
                L.tileLayer(TILE_URL, {
                    maxZoom: 19,
                    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                }).addTo(map);

                // Add attribution control in a clean spot
                L.control.attribution({ position: 'bottomright' }).addTo(map);
                
                // Layers and markers tracking
                let currentAlertId = null;
                let hasFittedBoundsForAlert = false;
                let markersLayerGroup = L.layerGroup().addTo(map);
                let accuracyLayerGroup = L.layerGroup().addTo(map);
                let markersByReplyId = {};

                function getMarkerColor(response, need) {
                    if (response === 'SAFE') return '#10B981'; // green
                    switch (need) {
                        case 'EMERGENCY': return '#991B1B'; // dark red
                        case 'MEDICAL': return '#DC2626';   // red
                        case 'FOOD_WATER': return '#EA580C';// orange
                        case 'SHELTER': return '#2563EB';   // blue
                        case 'OTHER': return '#6B7280';     // grey
                        case 'UNSPECIFIED': return '#6B7280';// grey
                        default: return '#6B7280';
                    }
                }

                function getFriendlyNeedLabel(response, need) {
                    if (response === 'SAFE') return 'Safe';
                    switch (need) {
                        case 'EMERGENCY': return 'Emergency / Rescue';
                        case 'MEDICAL': return 'Medicine';
                        case 'FOOD_WATER': return 'Food / Water';
                        case 'SHELTER': return 'Shelter';
                        case 'OTHER': return 'Other';
                        case 'UNSPECIFIED': return 'Not specified';
                        default: return 'Help Request';
                    }
                }

                function escapeHtml(str) {
                    if (!str) return '';
                    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
                }

                /**
                 * Called from Android with evaluateJavascript.
                 * Updates all markers on the map without losing map state or auto-fitting on refresh.
                 */
                window.setData = function(payload) {
                    try {
                        if (typeof payload === 'string') {
                            payload = JSON.parse(payload);
                        }
                        if (!payload) return;

                        // Check if alert changed
                        if (payload.alertId !== currentAlertId) {
                            currentAlertId = payload.alertId;
                            hasFittedBoundsForAlert = false;
                        }

                        markersLayerGroup.clearLayers();
                        accuracyLayerGroup.clearLayers();
                        markersByReplyId = {};

                        const items = payload.items || [];
                        const validLatLngs = [];

                        items.forEach(function(item) {
                            if (item.latitude === null || item.longitude === null || isNaN(item.latitude) || isNaN(item.longitude)) {
                                return; // no marker if no coords
                            }

                            const lat = item.latitude;
                            const lon = item.longitude;
                            validLatLngs.push([lat, lon]);

                            const color = getMarkerColor(item.response, item.need);
                            const needLabel = getFriendlyNeedLabel(item.response, item.need);
                            const isSimulated = (item.name && item.name.indexOf("Demo citizen") === 0);

                            // 1. Accuracy circle if accuracyM is provided
                            if (item.accuracyM !== null && item.accuracyM !== undefined && item.accuracyM > 0) {
                                L.circle([lat, lon], {
                                    radius: item.accuracyM,
                                    color: color,
                                    fillColor: color,
                                    fillOpacity: 0.12,
                                    weight: 1,
                                    interactive: false
                                }).addTo(accuracyLayerGroup);
                            }

                            // 2. Custom circular marker icon
                            const markerIcon = L.divIcon({
                                className: '',
                                html: '<div class="custom-marker" style="background-color: ' + color + '; width: 20px; height: 20px;"></div>',
                                iconSize: [20, 20],
                                iconAnchor: [10, 10],
                                popupAnchor: [0, -12]
                            });

                            const marker = L.marker([lat, lon], { icon: markerIcon }).addTo(markersLayerGroup);

                            // 3. Popup HTML
                            let popupHtml = '<div class="popup-title">';
                            if (isSimulated) {
                                popupHtml += '<span class="simulated-badge">SIMULATED</span> ';
                            }
                            popupHtml += escapeHtml(item.name) + '</div>';

                            popupHtml += '<span class="need-pill" style="background-color:' + color + ';">' + escapeHtml(needLabel) + '</span>';

                            if (item.priority) {
                                popupHtml += ' <span style="font-size:10px; font-weight:700; color:' + (item.priority === 'HIGH' ? '#DC2626' : '#EA580C') + ';">[' + escapeHtml(item.priority) + ' PRIORITY]</span>';
                            }

                            if (item.city) {
                                popupHtml += '<div class="popup-meta">📍 ' + escapeHtml(item.city) + '</div>';
                            }

                            if (item.note) {
                                popupHtml += '<div class="popup-note">"' + escapeHtml(item.note) + '"</div>';
                            }

                            // Time in IST
                            const timeText = item.istTime || item.at || '';
                            popupHtml += '<div class="popup-meta">🕒 ' + escapeHtml(timeText) + '</div>';

                            // Accuracy text
                            if (item.accuracyM !== null && item.accuracyM !== undefined) {
                                popupHtml += '<div class="popup-meta">🎯 Accuracy: ±' + Math.round(item.accuracyM) + 'm</div>';
                            } else {
                                popupHtml += '<div class="popup-meta">🎯 Accuracy: accuracy unknown</div>';
                            }

                            // Navigate link calling Android.onNavigate
                            const escapedName = escapeHtml(item.name).replace(/'/g, "\\'");
                            popupHtml += '<a href="javascript:void(0)" onclick="if(window.Android){Android.onNavigate(' + lat + ',' + lon + ',\'' + escapedName + '\');}" class="nav-btn">📍 Navigate</a>';

                            marker.bindPopup(popupHtml);

                            markersByReplyId[item.replyId] = {
                                marker: marker,
                                lat: lat,
                                lon: lon
                            };
                        });

                        // Fit bounds to markers on the FIRST load of an alert only, never on a refresh!
                        if (!hasFittedBoundsForAlert && validLatLngs.length > 0) {
                            setTimeout(function() {
                                if (window.map) {
                                    window.map.invalidateSize();
                                    window.map.fitBounds(L.latLngBounds(validLatLngs), {
                                        padding: [50, 50],
                                        maxZoom: 14
                                    });
                                }
                            }, 100);
                            hasFittedBoundsForAlert = true;
                        }
                    } catch (e) {
                        console.error("setData error: " + e);
                    }
                };

                /**
                 * Pans and zooms to a marker by replyId and opens its popup.
                 */
                window.focusOn = function(replyId) {
                    try {
                        const target = markersByReplyId[replyId];
                        if (target) {
                            map.setView([target.lat, target.lon], Math.max(map.getZoom(), 15), { animate: true });
                            target.marker.openPopup();
                        }
                    } catch (e) {
                        console.error("focusOn error: " + e);
                    }
                };
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}
