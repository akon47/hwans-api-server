package com.hwans.apiserver.service.ai.cover;

import lombok.Getter;

import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 커버 이미지 오른쪽에 그리는 단순한 선 그림. 200×200 좌표계에 그린다(호출하는 쪽이 위치·크기·선 굵기·색을 정한다).
 */
@Getter
public enum CoverIcon {
    SHIELD("shield") {
        @Override
        void draw(Graphics2D g) {
            var p = new Path2D.Double();
            p.moveTo(100, 18);
            p.lineTo(168, 44);
            p.lineTo(168, 98);
            p.curveTo(168, 142, 140, 170, 100, 186);
            p.curveTo(60, 170, 32, 142, 32, 98);
            p.lineTo(32, 44);
            p.closePath();
            g.draw(p);
            g.draw(polyline(70, 102, 92, 124, 134, 80));
        }
    },
    BUG("bug") {
        @Override
        void draw(Graphics2D g) {
            g.draw(new Ellipse2D.Double(78, 34, 44, 40));
            g.draw(new RoundRectangle2D.Double(58, 66, 84, 112, 84, 84));
            g.draw(new Line2D.Double(100, 80, 100, 166));
            g.draw(new Line2D.Double(58, 100, 28, 86));
            g.draw(new Line2D.Double(58, 126, 24, 126));
            g.draw(new Line2D.Double(58, 152, 28, 168));
            g.draw(new Line2D.Double(142, 100, 172, 86));
            g.draw(new Line2D.Double(142, 126, 176, 126));
            g.draw(new Line2D.Double(142, 152, 172, 168));
            g.draw(new Line2D.Double(88, 38, 72, 16));
            g.draw(new Line2D.Double(112, 38, 128, 16));
        }
    },
    LOCK("lock") {
        @Override
        void draw(Graphics2D g) {
            var shackle = new Path2D.Double();
            shackle.moveTo(64, 92);
            shackle.lineTo(64, 66);
            shackle.curveTo(64, 22, 136, 22, 136, 66);
            shackle.lineTo(136, 92);
            g.draw(shackle);
            g.draw(new RoundRectangle2D.Double(42, 92, 116, 90, 22, 22));
            g.fill(new Ellipse2D.Double(89, 116, 22, 22));
            g.draw(new Line2D.Double(100, 134, 100, 158));
        }
    },
    CODE("code") {
        @Override
        void draw(Graphics2D g) {
            g.draw(polyline(70, 58, 30, 100, 70, 142));
            g.draw(polyline(130, 58, 170, 100, 130, 142));
            g.draw(new Line2D.Double(114, 42, 86, 158));
        }
    },
    SERVER("server") {
        @Override
        void draw(Graphics2D g) {
            for (var i = 0; i < 3; i++) {
                var y = 28 + i * 50;
                g.draw(new RoundRectangle2D.Double(30, y, 140, 40, 14, 14));
                g.fill(new Ellipse2D.Double(44, y + 13, 14, 14));
                g.draw(new Line2D.Double(116, y + 20, 150, y + 20));
            }
        }
    },
    CLOUD("cloud") {
        @Override
        void draw(Graphics2D g) {
            var area = new Area(new Ellipse2D.Double(28, 88, 64, 64));
            area.add(new Area(new Ellipse2D.Double(62, 50, 86, 86)));
            area.add(new Area(new Ellipse2D.Double(112, 80, 62, 62)));
            area.add(new Area(new RoundRectangle2D.Double(28, 110, 146, 42, 42, 42)));
            g.draw(area);
        }
    },
    CHART("chart") {
        @Override
        void draw(Graphics2D g) {
            g.draw(polyline(34, 28, 34, 170, 176, 170));
            g.fill(new RoundRectangle2D.Double(56, 118, 24, 40, 8, 8));
            g.fill(new RoundRectangle2D.Double(96, 86, 24, 72, 8, 8));
            g.fill(new RoundRectangle2D.Double(136, 50, 24, 108, 8, 8));
        }
    },
    BOOK("book") {
        @Override
        void draw(Graphics2D g) {
            var left = new Path2D.Double();
            left.moveTo(100, 52);
            left.curveTo(78, 36, 50, 34, 24, 42);
            left.lineTo(24, 160);
            left.curveTo(50, 152, 78, 154, 100, 170);
            left.closePath();
            var right = new Path2D.Double();
            right.moveTo(100, 52);
            right.curveTo(122, 36, 150, 34, 176, 42);
            right.lineTo(176, 160);
            right.curveTo(150, 152, 122, 154, 100, 170);
            right.closePath();
            g.draw(left);
            g.draw(right);
        }
    },
    ROCKET("rocket") {
        @Override
        void draw(Graphics2D g) {
            var body = new Path2D.Double();
            body.moveTo(100, 16);
            body.curveTo(132, 42, 142, 86, 132, 132);
            body.lineTo(68, 132);
            body.curveTo(58, 86, 68, 42, 100, 16);
            body.closePath();
            g.draw(body);
            g.draw(new Ellipse2D.Double(86, 60, 28, 28));
            g.draw(polyline(70, 100, 42, 136, 42, 154, 68, 140));
            g.draw(polyline(130, 100, 158, 136, 158, 154, 132, 140));
            g.draw(polyline(84, 146, 100, 182, 116, 146));
        }
    },
    TERMINAL("terminal") {
        @Override
        void draw(Graphics2D g) {
            g.draw(new RoundRectangle2D.Double(18, 32, 164, 136, 20, 20));
            g.draw(new Line2D.Double(18, 64, 182, 64));
            for (var i = 0; i < 3; i++) {
                g.fill(new Ellipse2D.Double(32 + i * 18, 42, 11, 11));
            }
            g.draw(polyline(48, 90, 74, 112, 48, 134));
            g.draw(new Line2D.Double(90, 136, 138, 136));
        }
    },
    DATABASE("database") {
        @Override
        void draw(Graphics2D g) {
            g.draw(new Ellipse2D.Double(36, 22, 128, 40));
            g.draw(new Line2D.Double(36, 42, 36, 158));
            g.draw(new Line2D.Double(164, 42, 164, 158));
            g.draw(new Arc2D.Double(36, 80, 128, 40, 180, 180, Arc2D.OPEN));
            g.draw(new Arc2D.Double(36, 138, 128, 40, 180, 180, Arc2D.OPEN));
        }
    },
    NETWORK("network") {
        @Override
        void draw(Graphics2D g) {
            double[][] nodes = {{100, 36}, {36, 152}, {164, 152}, {100, 108}};
            int[][] edges = {{0, 3}, {1, 3}, {2, 3}, {0, 1}, {0, 2}, {1, 2}};
            for (var edge : edges) {
                g.draw(new Line2D.Double(nodes[edge[0]][0], nodes[edge[0]][1], nodes[edge[1]][0], nodes[edge[1]][1]));
            }
            for (var node : nodes) {
                g.fill(new Ellipse2D.Double(node[0] - 17, node[1] - 17, 34, 34));
            }
        }
    };

    private final String key;

    CoverIcon(String key) {
        this.key = key;
    }

    abstract void draw(Graphics2D g);

    public static Optional<CoverIcon> fromKey(String key) {
        return Arrays.stream(values()).filter(x -> x.key.equals(key)).findFirst();
    }

    public static List<String> keys() {
        return Arrays.stream(values()).map(CoverIcon::getKey).toList();
    }

    private static Path2D polyline(double... points) {
        var path = new Path2D.Double();
        path.moveTo(points[0], points[1]);
        for (var i = 2; i < points.length; i += 2) {
            path.lineTo(points[i], points[i + 1]);
        }
        return path;
    }
}
